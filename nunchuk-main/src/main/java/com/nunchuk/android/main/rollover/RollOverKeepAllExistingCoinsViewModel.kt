package com.nunchuk.android.main.rollover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.defaultRate
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.EstimateRollOverTransactionAndAmountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RollOverKeepAllExistingCoinsViewModel @Inject constructor(
    private val estimateRollOverTransactionAndAmountUseCase: EstimateRollOverTransactionAndAmountUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = RollOverKeepAllExistingCoinsFragmentArgs.fromSavedStateHandle(
        savedStateHandle
    )

    private val _uiState = MutableStateFlow(RollOverKeepAllExistingCoinsUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<RollOverKeepAllExistingCoinsEvent>()
    val event = _event.asSharedFlow()

    init {
        getEstimateFee()
    }

    private fun getEstimateFee() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        estimateFeeUseCase(Unit)
            .onSuccess { fee ->
                _uiState.update {
                    it.copy(manualFeeRate = fee.defaultRate)
                }
                calculateTransactionAndAmount()
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                _event.emit(RollOverKeepAllExistingCoinsEvent.Error(it.message.orEmpty()))
            }
    }

    private fun calculateTransactionAndAmount() {
        viewModelScope.launch {
            estimateRollOverTransactionAndAmountUseCase(
                EstimateRollOverTransactionAndAmountUseCase.Params(
                    args.oldWalletId,
                    args.newWalletId,
                    feeRate = _uiState.value.manualFeeRate.toManualFeeRate()
                )
            ).onSuccess { result ->
                _uiState.update {
                    it.copy(
                        numOfTxs = result.numOfTxs,
                        feeAmount = result.feeAmount
                    )
                }
            }
                .onFailure {
                    _event.emit(RollOverKeepAllExistingCoinsEvent.Error(it.message.orEmpty()))
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

internal fun Int.toManualFeeRate() = if (this > 0) toAmount() else Amount(-1)

sealed class RollOverKeepAllExistingCoinsEvent {
    data class Error(val message: String) : RollOverKeepAllExistingCoinsEvent()
}

data class RollOverKeepAllExistingCoinsUiState(
    val numOfTxs: Int = 0,
    val feeAmount: Amount = Amount.ZER0,
    val manualFeeRate: Int = -1,
    val isLoading: Boolean = false,
)

