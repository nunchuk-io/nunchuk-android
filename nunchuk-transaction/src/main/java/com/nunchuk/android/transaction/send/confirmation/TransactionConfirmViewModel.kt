package com.nunchuk.android.transaction.send.confirmation

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmEvent.*
import com.nunchuk.android.usecase.CreateTransactionUseCase
import com.nunchuk.android.usecase.DeleteTransactionUseCase
import com.nunchuk.android.usecase.DraftTransactionUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class TransactionConfirmViewModel @Inject constructor(
    private val draftTransactionUseCase: DraftTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : NunchukViewModel<Unit, TransactionConfirmEvent>() {

    private var manualFeeRate: Int = -1
    private lateinit var walletId: String
    private lateinit var address: String
    private var sendAmount: Double = 0.0
    private var estimateFee: Double = 0.0
    private var subtractFeeFromAmount: Boolean = false
    private lateinit var privateNote: String

    private lateinit var tempTxId: String

    override val initialState = Unit

    fun init(
        walletId: String,
        address: String,
        sendAmount: Double,
        estimateFee: Double,
        subtractFeeFromAmount: Boolean,
        privateNote: String,
        manualFeeRate: Int
    ) {
        this.walletId = walletId
        this.address = address
        this.sendAmount = sendAmount
        this.estimateFee = estimateFee
        this.subtractFeeFromAmount = subtractFeeFromAmount
        this.privateNote = privateNote
        this.manualFeeRate = manualFeeRate
        draftTransaction()
    }

    private fun draftTransaction() {
        viewModelScope.launch {
            when (val result = draftTransactionUseCase.execute(
                walletId = walletId,
                outputs = mapOf(address to sendAmount.toAmount()),
                subtractFeeFromAmount = subtractFeeFromAmount,
                feeRate = manualFeeRate.toManualFeeRate()
            )) {
                is Success -> onDraftTransactionSuccess(result.data)
                is Error -> event(CreateTxErrorEvent(result.exception.message.orEmpty()))
            }
        }

    }

    private fun onDraftTransactionSuccess(data: Transaction) {
        tempTxId = data.txId
        if (data.changeIndex > 0) {
            val txOutput = data.outputs[data.changeIndex]
            event(UpdateChangeAddress(txOutput.first, txOutput.second))
        }
    }

    fun handleConfirmEvent() {
        deleteDraftTransaction()
    }

    private fun deleteDraftTransaction() {
        viewModelScope.launch {
            when (val result = deleteTransactionUseCase.execute(walletId, tempTxId)) {
                is Success -> createNewTransaction()
                is Error -> event(CreateTxErrorEvent(result.exception.message.orEmpty()))
            }
        }
    }

    private fun createNewTransaction() {
        viewModelScope.launch {
            when (val result = createTransactionUseCase.execute(
                walletId = walletId,
                outputs = mapOf(address to sendAmount.toAmount()),
                subtractFeeFromAmount = subtractFeeFromAmount,
                feeRate = manualFeeRate.toManualFeeRate(),
                memo = privateNote
            )) {
                is Success -> event(CreateTxSuccessEvent(result.data.txId))
                is Error -> event(CreateTxErrorEvent(result.exception.message.orEmpty()))
            }
        }
    }

    private fun Int.toManualFeeRate() = if (this > 0) this.toAmount() else Amount(-1)

}