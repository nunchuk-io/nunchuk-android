package com.nunchuk.android.settings.walletsecurity.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.CheckWalletPinUseCase
import com.nunchuk.android.core.domain.CreateOrUpdateWalletPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnlockPinViewModel @Inject constructor(
    private val createOrUpdateWalletPinUseCase: CreateOrUpdateWalletPinUseCase,
    private val checkWalletPinUseCase: CheckWalletPinUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(UnlockPinUiState())
    val state = _state.asStateFlow()

    fun removePin(currentPin: String) {
        checkPin(currentPin) {
            createOrUpdateWalletPinUseCase("")
                .onSuccess {
                    _state.update { it.copy(isSuccess = true) }
                }
        }
    }

    fun unlockPin(pin: String) {
        checkPin(pin) {
            _state.update { it.copy(isSuccess = true) }
        }
    }

    private fun checkPin(pin: String, onSuccess: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isFailed = false) }
            checkWalletPinUseCase(pin)
                .onSuccess {
                    if (it) {
                        onSuccess()
                    } else {
                        _state.update { it.copy(isFailed = true, attemptCount = it.attemptCount.inc()) }
                    }
                }.onFailure {
                    _state.update { it.copy(isFailed = true, attemptCount = it.attemptCount.inc()) }
                }
        }
    }
}

data class UnlockPinUiState(
    val isFailed: Boolean = false,
    val attemptCount: Int = 0,
    val isSuccess: Boolean = false
)