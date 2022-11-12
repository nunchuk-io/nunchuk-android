package com.nunchuk.android.main.membership.key

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.membership.RestartWizardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddKeyStepViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val restartWizardUseCase: RestartWizardUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<AddKeyStepEvent>()
    val event = _event.asSharedFlow()

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

    val groupRemainTime =
        membershipStepManager.remainingTime.map {
            intArrayOf(
                membershipStepManager.getRemainTimeBySteps(
                    listOf(
                        MembershipStep.ADD_TAP_SIGNER_1,
                        MembershipStep.ADD_TAP_SIGNER_2,
                        MembershipStep.SETUP_KEY_RECOVERY
                    )
                ),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.SETUP_KEY_RECOVERY)),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.CREATE_WALLET)),
                membershipStepManager.getRemainTimeBySteps(listOf(MembershipStep.SETUP_INHERITANCE)),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, IntArray(3))

    fun openContactUs(email: String) {
        viewModelScope.launch {
            _event.emit(AddKeyStepEvent.OpenContactUs(email))
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            if (isSetupRecoverKeyDone.value) {
                _event.emit(AddKeyStepEvent.OpenCreateWallet)
            } else if (isConfigKeyDone.value) {
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

    fun resetWizard() {
        viewModelScope.launch {
            val result = restartWizardUseCase(membershipStepManager.plan)
            if (result.isSuccess) {
                _event.emit(AddKeyStepEvent.RestartWizardSuccess)
            }
        }
    }
}

sealed class AddKeyStepEvent {
    data class OpenContactUs(val email: String) : AddKeyStepEvent()
    object OpenAddKeyList : AddKeyStepEvent()
    object OpenRecoveryQuestion : AddKeyStepEvent()
    object OpenCreateWallet : AddKeyStepEvent()
    object OnMoreClicked : AddKeyStepEvent()
    object RestartWizardSuccess : AddKeyStepEvent()
}