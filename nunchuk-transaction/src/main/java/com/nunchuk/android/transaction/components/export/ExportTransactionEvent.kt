package com.nunchuk.android.transaction.components.export

import android.graphics.Bitmap

sealed class ExportTransactionEvent {
    data class ExportToFileSuccess(val filePath: String) : ExportTransactionEvent()
    data class ExportTransactionError(val message: String) : ExportTransactionEvent()
    object LoadingEvent : ExportTransactionEvent()
}

data class ExportTransactionState(
    val qrCodeBitmap: List<Bitmap> = emptyList(),
    val filePath: String = "",
)