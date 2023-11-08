package com.nunchuk.android.main.membership.byzantine.payment.address.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhitelistAddressViewModel @Inject constructor(
    private val checkAddressValidUseCase: CheckAddressValidUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(WhitelistAddressUiState())
    val state = _state.asStateFlow()
    fun checkAddressValid(addresses: List<String>) {
        viewModelScope.launch {
            checkAddressValidUseCase(CheckAddressValidUseCase.Params(addresses = addresses))
                .onSuccess {
                    if (it.isEmpty()) {
                        _state.update { state -> state.copy(openNextScreenEvent = triggered) }
                    } else {
                        _state.update { state -> state.copy(invalidAddressEvent = triggered(it.first())) }
                    }
                }
        }
    }

    fun onInvalidAddressEventConsumed() {
        _state.update { state -> state.copy(invalidAddressEvent = consumed()) }
    }

    fun onOpenNextScreenEventConsumed() {
        _state.update { state -> state.copy(openNextScreenEvent = consumed) }
    }
}

data class WhitelistAddressUiState(
    val openNextScreenEvent: StateEvent = consumed,
    val invalidAddressEvent: StateEventWithContent<String> = consumed(),
)