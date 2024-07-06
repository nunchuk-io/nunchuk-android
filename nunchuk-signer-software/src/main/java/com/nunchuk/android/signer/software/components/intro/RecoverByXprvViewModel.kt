package com.nunchuk.android.signer.software.components.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.usecase.signer.IsValidXprvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoverByXprvViewModel @Inject constructor(
    private val isValidXprvUseCase: IsValidXprvUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(RecoverByXprvViewState())
    val state = _state.asStateFlow()

    fun validateXprv(xprv: String) {
        _state.update { it.copy(xprv = xprv) }
        viewModelScope.launch {
            isValidXprvUseCase(xprv).onSuccess { isValid ->
                if (isValid) {
                    _state.update { it.copy(event = StateEvent.Unit) }
                } else {
                    _state.update { it.copy(event = StateEvent.String("Invalid XPRV")) }
                }
            }.onFailure { e ->
                _state.update { it.copy(event = StateEvent.String(e.message.orEmpty())) }
            }
        }
    }

    fun onEventHandled() {
        _state.update { it.copy(event = StateEvent.None) }
    }
}

data class RecoverByXprvViewState(
    val xprv: String = "",
    val event: StateEvent = StateEvent.None,
)