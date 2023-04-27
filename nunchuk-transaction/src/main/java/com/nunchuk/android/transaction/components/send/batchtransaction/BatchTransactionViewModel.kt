package com.nunchuk.android.transaction.components.send.batchtransaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.util.fromCurrencyToBTC
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.transaction.components.utils.privateNote
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatchTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val checkAddressValidUseCase: CheckAddressValidUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase,
    ) : ViewModel() {

    private val args = BatchTransactionFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<BatchTransactionEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(BatchTransactionState())
    val state = _state.asStateFlow()

    private var availableAmountWithoutUnlocked: Double = 0.0
    private var hasLockedCoin: Boolean = false
    private val isFromSelectedCoin by lazy { args.unspentOutputs.isNotEmpty() }

    init {
        if (isFromSelectedCoin.not()) {
            checkLockedCoin(args.walletId)
        }
    }

    private fun checkLockedCoin(walletId: String) {
        viewModelScope.launch {
            _event.emit(BatchTransactionEvent.Loading(true))
            getAllCoinUseCase(walletId).onSuccess {
                hasLockedCoin = it.any { coin -> coin.isLocked }
                availableAmountWithoutUnlocked =
                    it.sumOf { coin -> if (coin.isLocked) 0.0 else coin.amount.pureBTC() }
            }
            _event.emit(BatchTransactionEvent.Loading(false))
        }
    }

    fun isEnableCreateTransaction(): Boolean {
        return _state.value.recipients.any { it.amount.isEmpty() || it.address.isEmpty() }.not()
    }

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            val result = parseBtcUriUseCase(content)
            if (result.isSuccess) {
                val btcUri = result.getOrThrow()
                if (btcUri.amount.value > 0) {
                    updateRecipient(
                        index = state.value.interactingIndex,
                        amount = btcUri.amount.pureBTC().toString(),
                        address = btcUri.address,
                        isBtc = true
                    )
                    updateNoteChange(btcUri.privateNote)
                } else {
                    updateRecipient(
                        index = state.value.interactingIndex,
                        address = btcUri.address,
                    )
                }
            } else {
                _event.emit(BatchTransactionEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun createTransaction(isCustomTx: Boolean) = viewModelScope.launch {
        val amount = getTotalAmount()
        if (amount <= 0 || amount > args.availableAmount) {
            _event.emit(BatchTransactionEvent.InsufficientFundsEvent)
        } else if (amount > availableAmountWithoutUnlocked && !isFromSelectedCoin) {
            _event.emit(BatchTransactionEvent.InsufficientFundsLockedCoinEvent)
        } else {
            val addressList = _state.value.recipients.map { it.address }
            val resultCheckAddress = checkAddressValidUseCase(CheckAddressValidUseCase.Params(addressList))
            if (resultCheckAddress.isSuccess && resultCheckAddress.getOrThrow().isEmpty()) {
                if (isCustomTx) {
                    _event.emit(BatchTransactionEvent.CheckAddressSuccess)
                } else {
                    val resultEstimateFee = estimateFeeUseCase(Unit)
                    if (resultEstimateFee.isSuccess) {
                        _event.emit(BatchTransactionEvent.GetFeeRateSuccess(resultEstimateFee.getOrThrow()))
                    } else {
                        _event.emit((BatchTransactionEvent.Error(resultEstimateFee.exceptionOrNull()?.message.orUnknownError())))
                    }
                }
            } else {
                _event.emit(BatchTransactionEvent.Error(resultCheckAddress.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun getTotalAmount() = _state.value.recipients.sumOf {
        if (it.isBtc) {
            it.amount.toDouble()
        } else {
            it.amount.toDouble().fromCurrencyToBTC()
        }
    }

    fun updateRecipient(
        index: Int,
        amount: String? = null,
        isBtc: Boolean? = null,
        address: String? = null
    ) {
        var recipient = _state.value.recipients[index]
        amount?.let {
            recipient = recipient.copy(amount = it)
        }
        isBtc?.let {
            recipient = recipient.copy(isBtc = it)
        }
        address?.let {
            recipient = recipient.copy(address = it)
        }
        val newRecipients = _state.value.recipients.toMutableList()
        newRecipients[index] = recipient
        _state.update { it.copy(recipients = newRecipients) }
    }

    fun getTxReceiptList() = _state.value.recipients.map {
        TxReceipt(
            address = it.address, amount = if (it.isBtc) {
                it.amount.toDouble()
            } else {
                it.amount.toDouble().fromCurrencyToBTC()
            }
        )
    }

    fun getNote() = _state.value.note

    fun addRecipient() {
        val newRecipients = _state.value.recipients.toMutableList()
        newRecipients.add(BatchTransactionState.Recipient.DEFAULT)
        _state.update { it.copy(recipients = newRecipients) }
    }

    fun removeRecipient(index: Int) {
        val newRecipients = _state.value.recipients.toMutableList()
        if (newRecipients.size == 1) return
        newRecipients.removeAt(index)
        _state.update { it.copy(recipients = newRecipients) }
    }

    fun updateNoteChange(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun setInteractingIndex(index: Int) {
        _state.update { it.copy(interactingIndex = index) }
    }
}