package com.nunchuk.android.signer.tapsigner.backup.verify.byapp.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackUpResultHealthyViewModel @Inject constructor(

) : ViewModel() {
    private val _event = MutableSharedFlow<BackUpResultHealthyEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(BackUpResultHealthyEvent.OnContinueClicked)
        }
    }
}

sealed class BackUpResultHealthyEvent {
    object OnContinueClicked : BackUpResultHealthyEvent()
}