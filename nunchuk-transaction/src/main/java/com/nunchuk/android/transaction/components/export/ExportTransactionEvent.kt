package com.nunchuk.android.transaction.components.export

sealed class ExportTransactionEvent {
    data class ExportToFileSuccess(val filePath: String) : ExportTransactionEvent()
    data class ExportTransactionError(val message: String) : ExportTransactionEvent()
    object LoadingEvent : ExportTransactionEvent()
}

data class ExportTransactionState(val qrcode: List<String> = emptyList(), val filePath: String = "")