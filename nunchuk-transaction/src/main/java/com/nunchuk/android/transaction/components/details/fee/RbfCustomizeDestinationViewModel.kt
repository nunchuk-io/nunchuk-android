package com.nunchuk.android.transaction.components.details.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RbfCustomizeDestinationViewModel @Inject constructor(
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(RbfCustomizeDestinationUiState())
    val state = _state.asStateFlow()

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            parseBtcUriUseCase(content)
                .onSuccess {
                    _state.update { state -> state.copy(address = it.address) }
                }.onFailure { e ->
                    _state.update { state -> state.copy(errorMessage = e.message.orUnknownError()) }
                }
        }
    }

    fun onAddressChange(address: String) {
        _state.update { state -> state.copy(address = address) }
    }

    fun onHandledMessage() {
        _state.update { state -> state.copy(errorMessage = "") }
    }
}

data class RbfCustomizeDestinationUiState(
    val address: String = "",
    val errorMessage: String = "",
)