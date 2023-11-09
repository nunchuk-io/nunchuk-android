package com.nunchuk.android.main.membership.byzantine.payment.address.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhitelistAddressViewModel @Inject constructor(
    private val checkAddressValidUseCase: CheckAddressValidUseCase,
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(WhitelistAddressUiState())
    val state = _state.asStateFlow()
    fun checkAddressValid(addresses: List<String>) {
        viewModelScope.launch {
            checkAddressValidUseCase(CheckAddressValidUseCase.Params(addresses = addresses))
                .onSuccess {
                    if (it.isEmpty()) {
                        _state.update { state -> state.copy(openPaymentFrequentScreenEvent = it) }
                    } else {
                        _state.update { state -> state.copy(invalidAddressEvent = it.first()) }
                    }
                }.onFailure {
                    _state.update { state -> state.copy(invalidAddressEvent = addresses.first()) }
                }
        }
    }

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            parseBtcUriUseCase(content)
                .onSuccess {
                    _state.update { state -> state.copy(parseAddressEvent = it.address) }
                }.onFailure {
                    _state.update { state -> state.copy(parseAddressEvent = null) }
                }
        }
    }

    fun onInvalidAddressEventConsumed() {
        _state.update { state -> state.copy(invalidAddressEvent = null) }
    }

    fun onOpenNextScreenEventConsumed() {
        _state.update { state -> state.copy(openPaymentFrequentScreenEvent = null) }
    }

    fun onParseAddressEventConsumed() {
        _state.update { state -> state.copy(parseAddressEvent = null) }
    }
}

data class WhitelistAddressUiState(
    val isLoading: Boolean = false,
    val openPaymentFrequentScreenEvent: List<String>? = null,
    val invalidAddressEvent: String? = null,
    val parseAddressEvent: String? = null
)