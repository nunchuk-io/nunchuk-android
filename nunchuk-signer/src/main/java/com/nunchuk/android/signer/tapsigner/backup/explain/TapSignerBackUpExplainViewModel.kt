package com.nunchuk.android.signer.tapsigner.backup.explain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TapSignerBackUpExplainViewModel @Inject constructor(

) : ViewModel() {
    private val _event = MutableSharedFlow<TapSignerBackUpExplainEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(OnContinueClicked)
        }
    }
}

sealed class TapSignerBackUpExplainEvent

object OnContinueClicked : TapSignerBackUpExplainEvent()
