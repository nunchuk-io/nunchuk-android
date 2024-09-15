package com.nunchuk.android.settings.walletsecurity.decoy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import com.nunchuk.android.nativelib.NunchukNativeSdk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DecoyPinViewModel @Inject constructor(
    private val getWalletPinUseCase: GetWalletPinUseCase,
    private val nativeSdk: NunchukNativeSdk,
) : ViewModel() {
    private val _state = MutableStateFlow(DecoyPinUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getWalletPinUseCase(Unit)
                .map { it.getOrDefault("") }
                .collect { pin ->
                    _state.update { it.copy(walletPin = pin) }
                }
        }
    }

    fun getHashedPin(pin: String): String {
        return nativeSdk.hashSHA256(pin)
    }
}

data class DecoyPinUiState(
    val walletPin: String = "",
)