package com.nunchuk.android.transaction.components.details.fee

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.transaction.R
import com.nunchuk.android.usecase.CheckAddressValidUseCase
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
    private val checkAddressValidUseCase: CheckAddressValidUseCase,
    private val application: Application,
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

    fun checkAddressValid() {
        viewModelScope.launch {
            checkAddressValidUseCase(
                CheckAddressValidUseCase.Params(listOf(_state.value.address))
            ).onSuccess {
                if (it.isNotEmpty()) {
                    _state.update { state -> state.copy(errorMessage = application.getString(R.string.nc_transaction_invalid_address)) }
                } else {
                    _state.update { state -> state.copy(checkAddressSuccess = true) }
                }
            }
        }
    }

    fun onAddressChange(address: String) {
        _state.update { state -> state.copy(address = address) }
    }

    fun onHandledMessage() {
        _state.update { state -> state.copy(errorMessage = null) }
    }

    fun onHandledCheckAddressSuccess() {
        _state.update { state -> state.copy(checkAddressSuccess = false) }
    }
}

data class RbfCustomizeDestinationUiState(
    val address: String = "",
    val errorMessage: String? = null,
    val checkAddressSuccess: Boolean = false,
)