package com.nunchuk.android.transaction.send.confirmation

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmEvent.CreateTxErrorEvent
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmEvent.CreateTxSuccessEvent
import com.nunchuk.android.usecase.CreateTransactionUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class TransactionConfirmViewModel @Inject constructor(
    private val createTransactionUseCase: CreateTransactionUseCase,
) : NunchukViewModel<TransactionConfirmState, TransactionConfirmEvent>() {

    lateinit var walletId: String
    lateinit var address: String
    private var sendAmount: Double = 0.0
    private var estimateFee: Double = 0.0
    private var subtractFeeFromAmount: Boolean = false

    override val initialState = TransactionConfirmState()

    fun init(walletId: String, address: String, sendAmount: Double, estimateFee: Double, subtractFeeFromAmount: Boolean) {
        this.walletId = walletId
        this.address = address
        this.sendAmount = sendAmount
        this.estimateFee = estimateFee
        this.subtractFeeFromAmount = subtractFeeFromAmount
    }

    fun handleConfirmEvent() {
        viewModelScope.launch {
            when (val result = createTransactionUseCase.execute(
                walletId = walletId,
                outputs = mapOf(address to sendAmount.toAmount()),
                subtractFeeFromAmount = subtractFeeFromAmount
            )) {
                is Success -> event(CreateTxSuccessEvent(result.data.txId))
                is Error -> event(CreateTxErrorEvent(result.exception.message.orEmpty()))
            }
        }
    }

}