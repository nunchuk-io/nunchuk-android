package com.nunchuk.android.main.membership.honey.registerwallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.user.SetRegisterAirgapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterWalletToAirgapViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager,
    private val setRegisterAirgapUseCase: SetRegisterAirgapUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<RegisterWalletToAirgapEvent>()
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime

    fun onExportColdcardClicked() {
        viewModelScope.launch {
            _event.emit(RegisterWalletToAirgapEvent.ExportWalletToAirgap)
        }
    }

    fun setRegisterAirgapSuccess() {
        viewModelScope.launch {
            setRegisterAirgapUseCase(true)
        }
    }
}

sealed class RegisterWalletToAirgapEvent {
    object ExportWalletToAirgap : RegisterWalletToAirgapEvent()
}