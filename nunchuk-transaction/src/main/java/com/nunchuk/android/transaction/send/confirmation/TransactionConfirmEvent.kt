package com.nunchuk.android.transaction.send.confirmation

sealed class TransactionConfirmEvent {
    data class CreateTxSuccessEvent(val txId: String) : TransactionConfirmEvent()
    data class CreateTxErrorEvent(val message: String) : TransactionConfirmEvent()
}

class TransactionConfirmState