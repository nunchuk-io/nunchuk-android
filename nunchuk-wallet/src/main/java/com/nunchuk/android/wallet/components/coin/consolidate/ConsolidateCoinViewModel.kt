package com.nunchuk.android.wallet.components.coin.consolidate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.defaultRate
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class ConsolidateCoinUiState(
    val address: String = "",
    val manualFeeRate: Int = 0,
    val isLoading : Boolean = false,
    val showGenerateAddressError: Boolean = false
)

@HiltViewModel
class ConsolidateCoinViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUnusedWalletAddressUseCase: GetUnusedWalletAddressUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase
) : ViewModel() {
    private val args: ConsolidateCoinFragmentArgs =
        ConsolidateCoinFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _uiState = MutableStateFlow(ConsolidateCoinUiState())
    val uiState: StateFlow<ConsolidateCoinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getUnusedWalletAddressUseCase(args.walletId).onSuccess { addresses ->
                _uiState.update { state ->
                    state.copy(address = addresses.first())
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(showGenerateAddressError = true)
                }
            }
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true)
            }
            estimateFeeUseCase(Unit).onSuccess {
                _uiState.update { state ->
                    state.copy(manualFeeRate = it.defaultRate)
                }
            }
            _uiState.update { state ->
                state.copy(isLoading = false)
            }
        }
    }

    fun handleGenerateAddressError() {
        _uiState.update { state ->
            state.copy(showGenerateAddressError = false)
        }
    }
}