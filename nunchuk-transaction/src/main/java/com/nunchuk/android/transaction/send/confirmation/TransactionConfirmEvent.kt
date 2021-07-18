package com.nunchuk.android.transaction.send.confirmation

import com.nunchuk.android.model.Amount

sealed class TransactionConfirmEvent {
    object LoadingEvent : TransactionConfirmEvent()
    data class CreateTxSuccessEvent(val txId: String) : TransactionConfirmEvent()
    data class CreateTxErrorEvent(val message: String) : TransactionConfirmEvent()
    data class UpdateChangeAddress(val address: String, val amount: Amount) : TransactionConfirmEvent()
}