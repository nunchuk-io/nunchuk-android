package com.nunchuk.android.main.membership.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroAssistedWalletViewModel @Inject constructor() : ViewModel() {
    private val _event = MutableSharedFlow<IntroAssistedWalletEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(IntroAssistedWalletEvent.ContinueEvent)
        }
    }
}

sealed class IntroAssistedWalletEvent {
    object ContinueEvent : IntroAssistedWalletEvent()
}