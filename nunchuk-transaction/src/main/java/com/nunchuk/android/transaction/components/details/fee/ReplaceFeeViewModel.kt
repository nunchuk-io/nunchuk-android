package com.nunchuk.android.transaction.components.details.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.ReplaceTransactionUseCase
import com.nunchuk.android.usecase.EstimateFeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ReplaceFeeViewModel @Inject constructor(
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val replaceTransactionUseCase: ReplaceTransactionUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ReplaceFeeState())
    private val _event = MutableSharedFlow<ReplaceFeeEvent>()
    val event = _event.asSharedFlow()
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val result = estimateFeeUseCase(Unit)
            if (result.isSuccess) {
                _state.value = ReplaceFeeState(estimateFeeRates = result.getOrThrow())
            }
        }
    }

    fun validateFeeRate(feeRate: Int): Boolean {
        return feeRate >= _state.value.estimateFeeRates.minimumFee
    }

    fun replaceTransaction(walletId: String, txId: String, newFee: Int) {
        viewModelScope.launch {
            _event.emit(ReplaceFeeEvent.Loading(true))
            val result = replaceTransactionUseCase(ReplaceTransactionUseCase.Data(walletId, txId, newFee))
            _event.emit(ReplaceFeeEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(ReplaceFeeEvent.ReplaceTransactionSuccess(result.getOrThrow().txId))
            } else {
                _event.emit(ReplaceFeeEvent.ShowError(result.exceptionOrNull()))
            }
        }
    }
}