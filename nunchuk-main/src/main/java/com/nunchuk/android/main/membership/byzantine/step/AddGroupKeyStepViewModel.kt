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
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.byzantine.GetGroupBriefByIdFlowUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletUseCase
import com.nunchuk.android.usecase.membership.SyncGroupDraftWalletUseCase
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
    private val syncGroupDraftWalletUseCase: SyncGroupDraftWalletUseCase,
    private val syncGroupWalletUseCase: SyncGroupWalletUseCase,
    private val getGroupBriefByIdFlowUseCase: GetGroupBriefByIdFlowUseCase,
    getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val accountManager: AccountManager,
) : ViewModel() {
    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    private val walletId =
        savedStateHandle.getStateFlow(MembershipActivity.EXTRA_KEY_WALLET_ID, "")

    private val groupId =
        savedStateHandle.getStateFlow(MembershipActivity.EXTRA_GROUP_ID, "")

    private val _event = MutableSharedFlow<AddKeyStepEvent>()
    val event = _event.asSharedFlow()

    private val assistedWallets = getAssistedWalletsFlowUseCase(Unit)
        .map { it.getOrElse { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isRegisterAirgap = assistedWallets.combine(walletId) { assistedWallets, id ->
        (assistedWallets.find { it.localId == id }?.registerAirgapCount ?: 0) > 0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isRegisterColdcard = assistedWallets.combine(walletId) { assistedWallets, id ->
        (assistedWallets.find { it.localId == id }?.registerColdcardCount ?: 0) > 0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isConfigKeyDone = MutableStateFlow(false)
    val isConfigKeyDone = _isConfigKeyDone.asStateFlow()

    private val _isSetupRecoverKeyDone = MutableStateFlow(false)
    val isSetupRecoverKeyDone = _isSetupRecoverKeyDone.asStateFlow()

    private val _isCreateWalletDone = MutableStateFlow(false)
    val isCreateWalletDone = _isCreateWalletDone.asStateFlow()

    private val _isRequireInheritance = MutableStateFlow(false)
    val isRequireInheritance = _isRequireInheritance.asStateFlow()

    private val _uiState = MutableStateFlow(AddGroupUiState())

    val isSetupInheritanceDone =
        membershipStepManager.stepDone.map { membershipStepManager.isSetupInheritanceDone() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val groupRemainTime =
        membershipStepManager.remainingTime.map {
            intArrayOf(
                membershipStepManager.getRemainTimeByOtherSteps(
                    listOf(
                        MembershipStep.SETUP_KEY_RECOVERY,
                        MembershipStep.CREATE_WALLET,
                        MembershipStep.SETUP_INHERITANCE
                    )
                ),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.SETUP_KEY_RECOVERY)),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.CREATE_WALLET)),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.SETUP_INHERITANCE)),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, IntArray(4))

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            currentStep.filterNotNull().collect {
                membershipStepManager.setCurrentStep(it)
            }
        }
        viewModelScope.launch {
            getGroupBriefByIdFlowUseCase(groupId.value).collect {
                if (it.isSuccess) {
                    val email = accountManager.getAccount().email
                    _uiState.update { state ->
                        state.copy(isMaster = it.getOrThrow().members
                            .any { member -> member.emailOrUsername == email && member.role == AssistedWalletRole.MASTER.name })
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
                syncGroupDraftWalletUseCase(groupId.value)
            }
            val isCreateWallet = walletDeferred.await().isSuccess
            val draftWallet = draftWalletDeferred.await().getOrNull() ?: return@launch

            _isCreateWalletDone.value = isCreateWallet
            _isSetupRecoverKeyDone.value = draftWallet.isMasterSecurityQuestionSet
            val isConfigDone = draftWallet.config.n == draftWallet.signers.size
            _isConfigKeyDone.value = isCreateWallet || isConfigDone
            _isRequireInheritance.value = draftWallet.config.allowInheritance
        }
    }

    fun openContactUs(email: String) {
        viewModelScope.launch {
            _event.emit(AddKeyStepEvent.OpenContactUs(email))
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            if (isSetupInheritanceDone.value) {
                _event.emit(AddKeyStepEvent.SetupInheritanceSetupDone)
            } else if (isCreateWalletDone.value && isRegisterWalletDone()) {
                savedStateHandle[KEY_CURRENT_STEP] = MembershipStep.SETUP_INHERITANCE
                _event.emit(AddKeyStepEvent.OpenInheritanceSetup)
            } else if (isSetupRecoverKeyDone.value && isConfigKeyDone.value) {
                savedStateHandle[KEY_CURRENT_STEP] = MembershipStep.CREATE_WALLET
                if (isCreateWalletDone.value && isRegisterColdcard.value.not()) {
                    val walletId = assistedWallets.value.lastOrNull()?.localId ?: return@launch
                    _event.emit(
                        AddKeyStepEvent.OpenRegisterColdCard(
                            walletId,
                        )
                    )
                } else if (isCreateWalletDone.value && isRegisterAirgap.value.not()) {
                    val walletId = assistedWallets.value.lastOrNull()?.localId ?: return@launch
                    _event.emit(AddKeyStepEvent.OpenRegisterAirgap(walletId))
                } else {
                    _event.emit(AddKeyStepEvent.OpenCreateWallet)
                }
            } else if (isConfigKeyDone.value) {
                savedStateHandle[KEY_CURRENT_STEP] = MembershipStep.SETUP_KEY_RECOVERY
                _event.emit(AddKeyStepEvent.OpenRecoveryQuestion)
            } else {
                _event.emit(AddKeyStepEvent.OpenAddKeyList)
            }
        }
    }

    private fun isRegisterWalletDone() = assistedWallets.value.lastOrNull()
        ?.let { it.registerAirgapCount <= 0 && it.registerColdcardCount <= 0 } == true

    fun onMoreClicked() {
        viewModelScope.launch {
            _event.emit(AddKeyStepEvent.OnMoreClicked)
        }
    }

    fun activeWalletId() = walletId.value

    fun getRegisterColdcardIndex() =
        assistedWallets.value.find { it.localId == walletId.value }?.registerColdcardCount ?: 0

    fun getRegisterAirgapIndex() =
        assistedWallets.value.find { it.localId == walletId.value }?.registerAirgapCount ?: 0

    fun isMaster(): Boolean = _uiState.value.isMaster

    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
    }
}

data class AddGroupUiState(val isMaster: Boolean = false)

sealed class AddKeyStepEvent {
    data class OpenContactUs(val email: String) : AddKeyStepEvent()
    object OpenAddKeyList : AddKeyStepEvent()
    object OpenRecoveryQuestion : AddKeyStepEvent()
    object OpenCreateWallet : AddKeyStepEvent()
    data class OpenRegisterColdCard(
        val walletId: String,
    ) : AddKeyStepEvent()

    data class OpenRegisterAirgap(val walletId: String) : AddKeyStepEvent()
    object OnMoreClicked : AddKeyStepEvent()
    object OpenInheritanceSetup : AddKeyStepEvent()
    object SetupInheritanceSetupDone : AddKeyStepEvent()
}