package com.nunchuk.android.transaction.export

sealed class ExportTransactionEvent {
    data class ExportToFileSuccess(val filePath: String) : ExportTransactionEvent()
    data class ExportTransactionError(val message: String) : ExportTransactionEvent()
}

data class ExportTransactionState(val qrcode: List<String> = emptyList(), val filePath: String = "")