package com.nunchuk.android.main.components.tabs.services.emergencylockdown.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyLockdownIntroViewModel @Inject constructor() : ViewModel() {

    private val _event = MutableSharedFlow<EmergencyLockdownIntroEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(EmergencyLockdownIntroEvent.ContinueClick)
        }
    }

}