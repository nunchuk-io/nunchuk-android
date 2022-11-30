package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownsuccess

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class EmergencyLockdownSuccessViewModel @Inject constructor() : ViewModel() {

    private val _event = MutableSharedFlow<EmergencyLockdownSuccessEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
//        viewModelScope.launch {
//            _event.emit(KeyRecoverySuccessEvent.ContinueClick)
//        }
    }

}