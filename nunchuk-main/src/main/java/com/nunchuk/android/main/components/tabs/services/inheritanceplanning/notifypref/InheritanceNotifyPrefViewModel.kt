package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.main.components.tabs.services.keyrecovery.keyrecoverysuccess.KeyRecoverySuccessEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceNotifyPrefViewModel @Inject constructor() : ViewModel() {

    private val _event = MutableSharedFlow<InheritanceNotifyPrefEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(InheritanceNotifyPrefEvent.ContinueClick)
        }
    }

}