package com.nunchuk.android.transaction.components.imports

sealed class ImportTransactionEvent {
    object ImportTransactionSuccess : ImportTransactionEvent()
    data class ImportTransactionError(val message: String) : ImportTransactionEvent()
}