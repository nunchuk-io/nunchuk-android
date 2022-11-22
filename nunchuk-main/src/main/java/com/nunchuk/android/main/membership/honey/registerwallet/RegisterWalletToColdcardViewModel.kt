package com.nunchuk.android.main.membership.honey.registerwallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterWalletToColdcardViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager
) : ViewModel() {
    private val _event = MutableSharedFlow<RegisterWalletToColdcardEvent>()
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime

    fun onExportColdcardClicked() {
        viewModelScope.launch {
            _event.emit(RegisterWalletToColdcardEvent.ExportWalletToColdcard)
        }
    }
}

sealed class RegisterWalletToColdcardEvent {
    object ExportWalletToColdcard : RegisterWalletToColdcardEvent()
}