package com.nunchuk.android.transaction.components.send.confirmation

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction

sealed class TransactionConfirmEvent {
    object LoadingEvent : TransactionConfirmEvent()
    data class SweepLoadingEvent(val isLoading: Boolean) : TransactionConfirmEvent()
    data class CreateTxSuccessEvent(val txId: String) : TransactionConfirmEvent()
    data class CreateTxErrorEvent(val message: String) : TransactionConfirmEvent()
    data class UpdateChangeAddress(val address: String, val amount: Amount) : TransactionConfirmEvent()
    data class InitRoomTransactionError(val message: String) : TransactionConfirmEvent()
    data class InitRoomTransactionSuccess(val roomId: String) : TransactionConfirmEvent()
    data class Error(val e: Throwable?) : TransactionConfirmEvent()
    data class NfcLoading(val isLoading: Boolean) : TransactionConfirmEvent()
    data class SweepSuccess(val transaction: Transaction) : TransactionConfirmEvent()
}