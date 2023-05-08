package com.nunchuk.android.transaction.components.send.batchtransaction

import com.nunchuk.android.model.EstimateFeeRates

sealed class BatchTransactionEvent {
    data class Loading(val loading: Boolean) : BatchTransactionEvent()
    data class Error(val message: String) : BatchTransactionEvent()
    data class CheckAddressSuccess(val isCustomTx: Boolean) : BatchTransactionEvent()
    object InsufficientFundsEvent : BatchTransactionEvent()
    object InsufficientFundsLockedCoinEvent : BatchTransactionEvent()
}

data class BatchTransactionState(
    val note: String = "",
    val recipients: List<Recipient> = initRecipientList(),
    val interactingIndex: Int = -1
) {
    data class Recipient(
        val amount: String, val address: String, val isBtc: Boolean, val error: String
    ) {
        companion object {
            val DEFAULT = Recipient(amount = "", address = "", isBtc = true, error = "")
        }
    }
}

private fun initRecipientList(): List<BatchTransactionState.Recipient> {
    val recipients = arrayListOf<BatchTransactionState.Recipient>()
    recipients.add(BatchTransactionState.Recipient.DEFAULT)
    return recipients
}

