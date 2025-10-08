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

package com.nunchuk.android.main.membership.key

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.membership.GetSecurityQuestionUseCase
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddKeyStepViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val savedStateHandle: SavedStateHandle,
    private val getSecurityQuestionUseCase: GetSecurityQuestionUseCase,
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
) : ViewModel() {
    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    private val currentStage =
        savedStateHandle.getStateFlow(MembershipActivity.EXTRA_GROUP_STEP, MembershipStage.NONE)

    private val walletId =
        savedStateHandle.getStateFlow(MembershipActivity.EXTRA_KEY_WALLET_ID, "")

    private val _event = MutableSharedFlow<AddKeyStepEvent>()
    val event = _event.asSharedFlow()

    private val assistedWallets = getAssistedWalletsFlowUseCase(Unit)
        .map { it.getOrElse { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isRegisterAirgap = assistedWallets.combine(walletId) { assistedWallets, id ->
        (assistedWallets.find { it.localId == id }?.registerAirgapCount ?: 0) > 0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val plan = membershipStepManager.localMembershipPlan

    val isConfigKeyDone =
        membershipStepManager.stepDone.combine(currentStage) { _, stage ->
            membershipStepManager.isConfigKeyDone() || stage == MembershipStage.SETUP_INHERITANCE
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isSetupRecoverKeyDone = membershipStepManager.isConfigRecoverKeyDone

    val isCreateWalletDone =
        membershipStepManager.stepDone.combine(currentStage) { _, stage ->
            membershipStepManager.isCreatedAssistedWalletDone() || stage == MembershipStage.SETUP_INHERITANCE
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isSetupInheritanceDone =
        membershipStepManager.stepDone.map { membershipStepManager.isSetupInheritanceDone() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val groupRemainTime =
        membershipStepManager.remainingTime.map {
            val setupKeySteps = when (plan) {
                MembershipPlan.IRON_HAND -> listOf(
                    MembershipStep.IRON_ADD_HARDWARE_KEY_1,
                    MembershipStep.IRON_ADD_HARDWARE_KEY_2,
                    MembershipStep.ADD_SEVER_KEY
                )
                else -> listOf(
                    MembershipStep.HONEY_ADD_INHERITANCE_KEY,
                    MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
                    MembershipStep.HONEY_ADD_HARDWARE_KEY_2,
                    MembershipStep.ADD_SEVER_KEY
                )
            }
            intArrayOf(
                membershipStepManager.getRemainTimeBySteps(setupKeySteps),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.SETUP_KEY_RECOVERY)),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.CREATE_WALLET)),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.SETUP_INHERITANCE)),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, IntArray(4))

    var draftWalletType: WalletType? = null
        private set

    init {
        viewModelScope.launch {
            currentStep.filterNotNull().collect {
                membershipStepManager.setCurrentStep(it)
            }
        }
        viewModelScope.launch {
            getSecurityQuestionUseCase(GetSecurityQuestionUseCase.Param(isFilterAnswer = false,))
        }
        viewModelScope.launch {
            draftWalletType = syncDraftWalletUseCase("").getOrNull()?.walletType
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            if (isSetupInheritanceDone.value) {
                _event.emit(AddKeyStepEvent.SetupInheritanceSetupDone)
            } else if (isCreateWalletDone.value) {
                savedStateHandle[KEY_CURRENT_STEP] = MembershipStep.SETUP_INHERITANCE
                _event.emit(AddKeyStepEvent.OpenInheritanceSetup)
            } else if (isSetupRecoverKeyDone.value && isConfigKeyDone.value) {
                savedStateHandle[KEY_CURRENT_STEP] = MembershipStep.CREATE_WALLET
                if (isCreateWalletDone.value && isRegisterAirgap.value.not()) {
                    val walletId = assistedWallets.value.lastOrNull()?.localId ?: return@launch
                    _event.emit(AddKeyStepEvent.OpenRegisterAirgap(walletId))
                } else {
                    _event.emit(AddKeyStepEvent.OpenCreateWallet)
                }
            } else if (isConfigKeyDone.value) {
                savedStateHandle[KEY_CURRENT_STEP] = MembershipStep.SETUP_KEY_RECOVERY
                _event.emit(AddKeyStepEvent.OpenRecoveryQuestion)
            } else {
                _event.emit(AddKeyStepEvent.OpenAddKeyList(draftWalletType))
            }
        }
    }

    fun onMoreClicked() {
        viewModelScope.launch {
            _event.emit(AddKeyStepEvent.OnMoreClicked)
        }
    }

    fun activeWalletId() = walletId.value

    fun requireInheritance(walletId: String) {
        savedStateHandle[MembershipActivity.EXTRA_GROUP_STEP] = MembershipStage.SETUP_INHERITANCE
        savedStateHandle[MembershipActivity.EXTRA_KEY_WALLET_ID] = walletId
    }

    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
    }
}

sealed class AddKeyStepEvent {
    data class OpenContactUs(val email: String) : AddKeyStepEvent()
    data class OpenAddKeyList(val walletType: WalletType?) : AddKeyStepEvent()
    object OpenRecoveryQuestion : AddKeyStepEvent()
    object OpenCreateWallet : AddKeyStepEvent()
    data class OpenRegisterAirgap(val walletId: String) : AddKeyStepEvent()
    object OnMoreClicked : AddKeyStepEvent()
    object OpenInheritanceSetup : AddKeyStepEvent()
    object SetupInheritanceSetupDone : AddKeyStepEvent()
}