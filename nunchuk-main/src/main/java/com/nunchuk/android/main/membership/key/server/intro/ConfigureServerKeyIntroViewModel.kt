package com.nunchuk.android.main.membership.key.server.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigureServerKeyIntroViewModel @Inject constructor() : ViewModel() {
    private val _event = MutableSharedFlow<ConfigureServerKeyIntroEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(ConfigureServerKeyIntroEvent.ContinueStepEvent)
        }
    }
}

sealed class ConfigureServerKeyIntroEvent {
    data class Loading(val isLoading: Boolean) : ConfigureServerKeyIntroEvent()
    object ContinueStepEvent : ConfigureServerKeyIntroEvent()
}