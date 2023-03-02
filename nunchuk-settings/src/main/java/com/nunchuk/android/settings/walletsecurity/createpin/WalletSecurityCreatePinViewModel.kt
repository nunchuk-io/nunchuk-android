package com.nunchuk.android.settings.walletsecurity.createpin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.CheckWalletPinUseCase
import com.nunchuk.android.core.domain.CreateOrUpdateWalletPinUseCase
import com.nunchuk.android.model.setting.WalletSecuritySetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletSecurityCreatePinViewModel @Inject constructor(
    private val createOrUpdateWalletPinUseCase: CreateOrUpdateWalletPinUseCase,
    private val checkWalletPinUseCase: CheckWalletPinUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args = WalletSecurityCreatePinFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<WalletSecurityCreatePinEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(WalletSecurityCreatePinState())
    val state = _state.asStateFlow()

    init {
        val inputValue = hashMapOf<Int, String>()
        repeat((0..2).count()) {
            if (args.currentPin.isBlank() && it == 2) return@repeat
            inputValue[it] = ""
        }
        _state.update {
            it.copy(
                inputValue = inputValue,
                currentPin = args.currentPin,
                createPinFlow = args.currentPin.isBlank()
            )
        }
    }

    fun createOrUpdateWalletPin() = viewModelScope.launch {
        val inputValue = _state.value.inputValue
        if (_state.value.createPinFlow) {
            if (inputValue[0] == inputValue[1]) {
                createOrUpdateWalletPinUseCase(inputValue[0]!!)
                _event.emit(WalletSecurityCreatePinEvent.CreateOrUpdateSuccess)
            }
        } else {
            val matchPin = checkWalletPinUseCase(inputValue[0]!!)
            if (matchPin.getOrDefault(false) && inputValue[1] == inputValue[2]) {
                createOrUpdateWalletPinUseCase(inputValue[1]!!)
                _event.emit(WalletSecurityCreatePinEvent.CreateOrUpdateSuccess)
            }
        }
    }

    fun updateInputValue(index: Int, value: String) {
        val inputValue = _state.value.inputValue.toMutableMap()
        inputValue[index] = value
        _state.update { it.copy(inputValue = inputValue) }
    }
}

data class WalletSecurityCreatePinState(
    val walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting(),
    val inputValue: MutableMap<Int, String> = hashMapOf(),
    val currentPin: String = "",
    val createPinFlow: Boolean = false
)

sealed class WalletSecurityCreatePinEvent {
    data class Loading(val loading: Boolean) : WalletSecurityCreatePinEvent()
    data class Error(val message: String) : WalletSecurityCreatePinEvent()
    object CreateOrUpdateSuccess : WalletSecurityCreatePinEvent()
}