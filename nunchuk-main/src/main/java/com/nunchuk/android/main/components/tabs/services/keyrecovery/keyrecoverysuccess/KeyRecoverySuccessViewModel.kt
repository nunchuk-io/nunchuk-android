package com.nunchuk.android.main.components.tabs.services.keyrecovery.keyrecoverysuccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyRecoverySuccessViewModel @Inject constructor() : ViewModel() {

    private val _event = MutableSharedFlow<KeyRecoverySuccessEvent>()
    val event = _event.asSharedFlow()

    fun onGotItClick() {
        viewModelScope.launch {
            _event.emit(KeyRecoverySuccessEvent.GotItClick)
        }
    }

}