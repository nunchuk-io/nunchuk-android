package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownperiod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyLockdownPeriodViewModel @Inject constructor() : ViewModel() {

    private val _event = MutableSharedFlow<LockdownPeriodEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(LockdownPeriodState())
    val state = _state.asStateFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(LockdownPeriodEvent.ContinueClick)
        }
    }

}