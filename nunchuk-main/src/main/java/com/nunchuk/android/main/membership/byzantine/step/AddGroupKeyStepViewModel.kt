/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.main.membership.byzantine.step

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.util.InheritancePlanType
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.byzantine.GetGroupRemoteUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletUseCase
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddGroupKeyStepViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val savedStateHandle: SavedStateHandle,
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    private val syncGroupWalletUseCase: SyncGroupWalletUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val getGroupRemoteUseCase: GetGroupRemoteUseCase,
    getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val accountManager: AccountManager,
) : ViewModel() {
    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    private val walletId =
        savedStateHandle.getStateFlow(MembershipActivity.EXTRA_KEY_WALLET_ID, "")

    private val groupId =
        savedStateHandle.getStateFlow(MembershipFragment.EXTRA_GROUP_ID, "")

    private val inheritanceType = 
        savedStateHandle.getStateFlow(MembershipActivity.EXTRA_INHERITANCE_TYPE, null as String?)

    private val _event = MutableSharedFlow<AddKeyStepEvent>()
    val event = _event.asSharedFlow()

    private val assistedWallets = getAssistedWalletsFlowUseCase(Unit)
        .map { it.getOrElse { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isRegisterAirgap = assistedWallets.combine(walletId) { assistedWallets, id ->
        (assistedWallets.find { it.localId == id }?.registerAirgapCount ?: 0) > 0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isConfigKeyDone = MutableStateFlow(false)
    val isConfigKeyDone = _isConfigKeyDone.asStateFlow()

    private val _isSetupRecoverKeyDone = MutableStateFlow(false)
    val isSetupRecoverKeyDone = _isSetupRecoverKeyDone.asStateFlow()

    private val _isCreateWalletDone = MutableStateFlow(false)
    val isCreateWalletDone = _isCreateWalletDone.asStateFlow()

    private val _uiState = MutableStateFlow(AddGroupUiState())
    val uiState = _uiState.asStateFlow()

    private var draftWalletType: WalletType? = null

    val groupRemainTime =
        membershipStepManager.remainingTime.map {
            intArrayOf(
                membershipStepManager.getRemainTimeByOtherSteps(
                    listOf(
                        MembershipStep.SETUP_KEY_RECOVERY,
                        MembershipStep.CREATE_WALLET
                    )
                ),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.SETUP_KEY_RECOVERY)),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.CREATE_WALLET))
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, IntArray(3))

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            currentStep.filterNotNull().collect {
                membershipStepManager.setCurrentStep(it)
            }
        }
        viewModelScope.launch {
            getGroupRemoteUseCase(GetGroupRemoteUseCase.Params(groupId.value))
            getGroupUseCase(GetGroupUseCase.Params(groupId.value)).collect {
                if (it.isSuccess) {
                    val email = accountManager.getAccount().email
                    val role = it.getOrThrow().members
                        .find { member -> member.emailOrUsername == email }?.role
                        ?: AssistedWalletRole.NONE.name
                    _uiState.update { state ->
                        state.copy(isMaster = role == AssistedWalletRole.MASTER.name, role = role.toRole)
                    }
                }
            }
        }
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val walletDeferred = async {
                syncGroupWalletUseCase(groupId.value)
            }
            val draftWalletDeferred = async {
                syncDraftWalletUseCase(groupId.value)
            }
            val isCreateWallet = walletDeferred.await().isSuccess
            val draftWallet = draftWalletDeferred.await().getOrNull() ?: return@launch

            draftWallet.replaceWallet?.localId?.let { localId ->
                _event.emit(AddKeyStepEvent.UpdateReplaceWalletId(localId))
            }

            _isCreateWalletDone.value = isCreateWallet
            _isSetupRecoverKeyDone.value = draftWallet.isMasterSecurityQuestionSet
            val isConfigDone = if (draftWallet.walletType == WalletType.MINISCRIPT) {
                val isSignerCountCorrect = draftWallet.signers.size == draftWallet.config.n * 2 - 1
                val isTimelockSet = draftWallet.timelock > 0
                val areInheritanceSignersVerified = draftWallet.signers.all { signer ->
                    !signer.tags.contains(SignerTag.INHERITANCE.name) || signer.verifyType != VerifyType.NONE
                }
                val areNfcSignersVerified = draftWallet.signers.all { signer ->
                    signer.type != SignerType.NFC || signer.verifyType != VerifyType.NONE
                }
                isSignerCountCorrect && isTimelockSet && areInheritanceSignersVerified && areNfcSignersVerified
            } else {
                draftWallet.config.n == draftWallet.signers.size
            }
            _isConfigKeyDone.value = isCreateWallet || isConfigDone
            draftWalletType = draftWallet.walletType
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            if (isSetupRecoverKeyDone.value && isConfigKeyDone.value) {
                savedStateHandle[KEY_CURRENT_STEP] = MembershipStep.CREATE_WALLET
                if (isCreateWalletDone.value && isRegisterAirgap.value.not() && draftWalletType != WalletType.MINISCRIPT) {
                    val walletId = assistedWallets.value.lastOrNull()?.localId ?: return@launch
                    _event.emit(AddKeyStepEvent.OpenRegisterAirgap(walletId))
                } else {
                    _event.emit(AddKeyStepEvent.OpenCreateWallet)
                }
            } else if (isConfigKeyDone.value) {
                savedStateHandle[KEY_CURRENT_STEP] = MembershipStep.SETUP_KEY_RECOVERY
                _event.emit(AddKeyStepEvent.OpenRecoveryQuestion)
            } else {
                if (inheritanceType.value == InheritancePlanType.ON_CHAIN.name) {
                    _event.emit(AddKeyStepEvent.OpenOnChainTimelockExplanation)
                } else {
                    _event.emit(AddKeyStepEvent.OpenAddKeyList(draftWalletType))
                }
            }
        }
    }

    fun onMoreClicked() {
        viewModelScope.launch {
            _event.emit(AddKeyStepEvent.OnMoreClicked)
        }
    }

    fun isMaster(): Boolean = _uiState.value.isMaster

    fun getRole(): AssistedWalletRole = _uiState.value.role

    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
    }
}

data class AddGroupUiState(val isMaster: Boolean = false, val role: AssistedWalletRole = AssistedWalletRole.NONE)

sealed class AddKeyStepEvent {
    data class OpenAddKeyList(val walletType: WalletType?) : AddKeyStepEvent()
    object OpenRecoveryQuestion : AddKeyStepEvent()
    object OpenCreateWallet : AddKeyStepEvent()
    data class OpenRegisterAirgap(val walletId: String) : AddKeyStepEvent()
    object OnMoreClicked : AddKeyStepEvent()
    object OpenOnChainTimelockExplanation : AddKeyStepEvent()
    data class UpdateReplaceWalletId(val walletId: String) : AddKeyStepEvent()
}