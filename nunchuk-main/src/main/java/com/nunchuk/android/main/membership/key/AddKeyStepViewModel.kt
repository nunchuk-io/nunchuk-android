/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.user.IsRegisterAirgapUseCase
import com.nunchuk.android.usecase.user.IsRegisterColdcardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddKeyStepViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val savedStateHandle: SavedStateHandle,
    isRegisterColdcardUseCase: IsRegisterColdcardUseCase,
    isRegisterAirgapUseCase: IsRegisterAirgapUseCase,
    getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<AddKeyStepEvent>()
    val event = _event.asSharedFlow()

    val isRegisterAirgap = isRegisterAirgapUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isRegisterColdcard = isRegisterColdcardUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val assistedWallets = getAssistedWalletsFlowUseCase(Unit)
        .map { it.getOrElse { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    val plan = membershipStepManager.plan

    val isConfigKeyDone =
        membershipStepManager.stepDone.map { membershipStepManager.isConfigKeyDone() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isSetupRecoverKeyDone =
        membershipStepManager.stepDone.map { membershipStepManager.isConfigRecoverKeyDone() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isCreateWalletDone =
        membershipStepManager.stepDone.map { membershipStepManager.isCreatedAssistedWalletDone() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isSetupInheritanceDone =
        membershipStepManager.stepDone.map { membershipStepManager.isSetupInheritanceDone() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val groupRemainTime =
        membershipStepManager.remainingTime.map {
            val setupKeySteps = if (plan == MembershipPlan.IRON_HAND) listOf(
                MembershipStep.IRON_ADD_HARDWARE_KEY_1,
                MembershipStep.IRON_ADD_HARDWARE_KEY_2,
                MembershipStep.ADD_SEVER_KEY
            ) else listOf(
                MembershipStep.HONEY_ADD_TAP_SIGNER,
                MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
                MembershipStep.HONEY_ADD_HARDWARE_KEY_2,
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
            } else if (isCreateWalletDone.value && isRegisterAirgap.value && isRegisterColdcard.value) {
                savedStateHandle[KEY_CURRENT_STEP] = MembershipStep.SETUP_INHERITANCE
                _event.emit(AddKeyStepEvent.OpenInheritanceSetup)
            } else if (isSetupRecoverKeyDone.value && isConfigKeyDone.value) {
                savedStateHandle[KEY_CURRENT_STEP] = MembershipStep.CREATE_WALLET
                if (isRegisterColdcard.value.not()) {
                    val walletId = assistedWallets.value.find { it.isSetupInheritance }?.localId.orEmpty()
                    _event.emit(
                        AddKeyStepEvent.OpenRegisterColdCard(
                            walletId,
                            isRegisterAirgap.value
                        )
                    )
                } else if (isRegisterAirgap.value.not()) {
                    val walletId = assistedWallets.value.find { it.isSetupInheritance }?.localId.orEmpty()
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

    fun onMoreClicked() {
        viewModelScope.launch {
            _event.emit(AddKeyStepEvent.OnMoreClicked)
        }
    }

    fun unSetupWallet() = assistedWallets.value.find { it.isSetupInheritance.not() }

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