package com.nunchuk.android.settings.walletsecurity.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PinStatusViewModel @Inject constructor(
    private val getWalletPinUseCase: GetWalletPinUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(PinStatusUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getWalletPinUseCase(Unit)
                .map { it.getOrDefault("") }
                .catch { Timber.e(it) }
                .collect {
                    _state.update { state -> state.copy(pin = it) }
                }
        }

    }
}

data class PinStatusUiState(val pin: String = "")