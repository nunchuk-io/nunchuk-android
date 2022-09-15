package com.nunchuk.android.main.membership.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateWalletSuccessViewModel @Inject constructor() : ViewModel() {
    private val _event = MutableSharedFlow<CreateWalletSuccessEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(CreateWalletSuccessEvent.ContinueStepEvent)
        }
    }
}

sealed class CreateWalletSuccessEvent {
    object ContinueStepEvent : CreateWalletSuccessEvent()
}