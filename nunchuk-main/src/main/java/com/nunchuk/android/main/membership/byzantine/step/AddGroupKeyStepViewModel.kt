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
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.membership.GetSecurityQuestionUseCase
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletUseCase
import com.nunchuk.android.usecase.membership.SyncGroupDraftWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddGroupKeyStepViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val savedStateHandle: SavedStateHandle,
    private val getSecurityQuestionUseCase: GetSecurityQuestionUseCase,
    private val syncGroupDraftWalletUseCase: SyncGroupDraftWalletUseCase,
    private val syncGroupWalletUseCase: SyncGroupWalletUseCase,
    getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
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
        assistedWallets.find { it.localId == id }?.isRegisterAirgap == true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isRegisterColdcard = assistedWallets.combine(walletId) { assistedWallets, id ->
        assistedWallets.find { it.localId == id }?.isRegisterColdcard == true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isConfigKeyDone = MutableStateFlow(false)
    val isConfigKeyDone = _isConfigKeyDone.asStateFlow()

    val isSetupRecoverKeyDone = membershipStepManager.isConfigRecoverKeyDone

    private val _isCreateWalletDone = MutableStateFlow(false)
    val isCreateWalletDone = _isCreateWalletDone.asStateFlow()

    private val _isRequireInheritance = MutableStateFlow(false)
    val isRequireInheritance = _isRequireInheritance.asStateFlow()

    val isSetupInheritanceDone =
        membershipStepManager.stepDone.map { membershipStepManager.isSetupInheritanceDone() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val groupRemainTime =
        membershipStepManager.remainingTime.map {
            val setupKeySteps = listOf(
                MembershipStep.BYZANTINE_ADD_TAP_SIGNER,
                MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
                MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
                MembershipStep.ADD_SEVER_KEY
            )
            intArrayOf(
                membershipStepManager.getRemainTimeBySteps(setupKeySteps),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.SETUP_KEY_RECOVERY)),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.CREATE_WALLET)),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.SETUP_INHERITANCE)),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, IntArray(4))

    init {
        viewModelScope.launch {
            currentStep.filterNotNull().collect {
                membershipStepManager.setCurrentStep(it)
            }
        }
        viewModelScope.launch {
            getSecurityQuestionUseCase(GetSecurityQuestionUseCase.Param(isFilterAnswer = false))
        }
    }

    fun refresh() {
        viewModelScope.launch {
            syncGroupDraftWalletUseCase(groupId.value).onSuccess { draftWallet ->
                val isConfigDone = draftWallet.config.m == draftWallet.signers.size
                _isConfigKeyDone.value = isConfigDone
                _isRequireInheritance.value = draftWallet.config.allowInheritance
                if (isConfigDone) {
                    syncGroupWalletUseCase(groupId.value).onSuccess {
                        _isCreateWalletDone.value = true
                    }
                }
            }
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
                            isRegisterAirgap.value
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
        ?.let { it.isRegisterAirgap && it.isRegisterColdcard } == true

    fun onMoreClicked() {
        viewModelScope.launch {
            _event.emit(AddKeyStepEvent.OnMoreClicked)
        }
    }

    fun activeWalletId() = walletId.value

    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
    }
}

sealed class AddKeyStepEvent {
    data class OpenContactUs(val email: String) : AddKeyStepEvent()
    object OpenAddKeyList : AddKeyStepEvent()
    object OpenRecoveryQuestion : AddKeyStepEvent()
    object OpenCreateWallet : AddKeyStepEvent()
    data class OpenRegisterColdCard(
        val walletId: String,
        val isNeedRegisterAirgap: Boolean,
    ) : AddKeyStepEvent()

    data class OpenRegisterAirgap(val walletId: String) : AddKeyStepEvent()
    object OnMoreClicked : AddKeyStepEvent()
    object OpenInheritanceSetup : AddKeyStepEvent()
    object SetupInheritanceSetupDone : AddKeyStepEvent()
}