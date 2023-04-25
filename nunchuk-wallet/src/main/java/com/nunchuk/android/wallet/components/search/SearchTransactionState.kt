package com.nunchuk.android.wallet.components.search

import com.nunchuk.android.model.transaction.ExtendedTransaction

data class SearchTransactionState(
    val transactions: List<ExtendedTransaction> = emptyList(),
    val query: String = ""
)

sealed class SearchTransactionEvent {
    data class Error(val message: String) : SearchTransactionEvent()
}