package com.nunchuk.android.transaction.components.details.fee

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TxInput
import com.nunchuk.android.transaction.components.send.confirmation.toManualFeeRate
import com.nunchuk.android.usecase.DraftTransactionUseCase
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.GetDefaultAntiFeeSnipingUseCase
import com.nunchuk.android.usecase.coin.GetCoinsFromTxInputsUseCase
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RbfCancelTransactionViewModel @Inject constructor(
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val getUnusedWalletAddressUseCase: GetUnusedWalletAddressUseCase,
    private val draftTransactionUseCase: DraftTransactionUseCase,
    private val getCoinsFromTxInputsUseCase: GetCoinsFromTxInputsUseCase,
    private val getDefaultAntiFeeSnipingUseCase: GetDefaultAntiFeeSnipingUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val walletId = savedStateHandle.get<String>(ReplaceFeeArgs.EXTRA_WALLET_ID).orEmpty()
    private val oldTx = savedStateHandle.get<Transaction>(ReplaceFeeArgs.EXTRA_TRANSACTION)!!

    private val _state =
        MutableStateFlow(RbfCancelTransactionUiState(previousFeeRate = oldTx.feeRate.value.toInt()))
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<ReplaceFeeEvent>()
    val event = _event.asSharedFlow()

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
        viewModelScope.launch {
            getDefaultAntiFeeSnipingUseCase(Unit)
                .collect { result ->
                    _state.update {
                        it.copy(antiFeeSniping = result.getOrDefault(false))
                    }
                }
        }
    }

    fun draftCancelTransaction(newFee: Int, address: String) {
        viewModelScope.launch {
            _event.emit(ReplaceFeeEvent.Loading(true))
            getCoinsFromTxInputsUseCase(
                GetCoinsFromTxInputsUseCase.Params(
                    walletId = walletId,
                    txInputs = oldTx.inputs
                )
            ).onSuccess { coins ->
                draftTransactionUseCase(
                    DraftTransactionUseCase.Params(
                        walletId = walletId,
                        inputs = coins.map { TxInput(it.txid, it.vout) },
                        outputs = mapOf(address to coins.sumOf { it.amount.value }.toAmount()),
                        subtractFeeFromAmount = true,
                        feeRate = newFee.toManualFeeRate(),
                        replaceTxId = oldTx.txId
                    )
                ).onSuccess { transaction ->
                    _event.emit(ReplaceFeeEvent.DraftTransactionSuccess(transaction, newFee))
                }.onFailure {
                    _event.emit(ReplaceFeeEvent.ShowError(it))
                }
            }.onFailure {
                _event.emit(ReplaceFeeEvent.ShowError(it))
            }
            _event.emit(ReplaceFeeEvent.Loading(false))
        }
    }

    fun onAntiFeeSnipingChange() {
        _state.update { it.copy(antiFeeSniping = !it.antiFeeSniping) }
    }
}

data class RbfCancelTransactionUiState(
    val fee: EstimateFeeRates = EstimateFeeRates(),
    val address: String = "",
    val previousFeeRate: Int = 0,
    val antiFeeSniping: Boolean = false,
)