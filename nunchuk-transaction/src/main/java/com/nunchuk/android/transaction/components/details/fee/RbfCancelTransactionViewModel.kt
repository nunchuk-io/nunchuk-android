package com.nunchuk.android.transaction.components.details.fee

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RbfCancelTransactionViewModel @Inject constructor(
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val getUnusedWalletAddressUseCase: GetUnusedWalletAddressUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val walletId = savedStateHandle.get<String>(ReplaceFeeArgs.EXTRA_WALLET_ID).orEmpty()
    private val transaction = savedStateHandle.get<Transaction>(ReplaceFeeArgs.EXTRA_TRANSACTION)!!

    private val _state = MutableStateFlow(RbfCancelTransactionUiState(previousFeeRate = transaction.feeRate.value.toInt()))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            estimateFeeUseCase(Unit).onSuccess { fee ->
                _state.update { it.copy(fee = fee) }
            }
        }
        viewModelScope.launch {
            getUnusedWalletAddressUseCase(walletId).onSuccess { address ->
                _state.update { it.copy(address = address.firstOrNull().orEmpty()) }
            }
        }
    }

    fun onAddressChange(address: String) {
        _state.update { it.copy(address = address) }
    }
}

data class RbfCancelTransactionUiState(
    val fee: EstimateFeeRates = EstimateFeeRates(),
    val address: String = "",
    val previousFeeRate: Int = 0,
)