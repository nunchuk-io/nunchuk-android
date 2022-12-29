package com.nunchuk.android.main.membership.authentication.dummytx

data class DummyTransactionState(
    val viewMore: Boolean = false,
)

sealed class DummyTransactionEvent {
    data class ShowMoreOption(
        val isPendingTransaction: Boolean,
        val isPendingConfirm: Boolean,
        val isRejected: Boolean,
        val masterFingerPrint: String,
    ) : DummyTransactionEvent()
}