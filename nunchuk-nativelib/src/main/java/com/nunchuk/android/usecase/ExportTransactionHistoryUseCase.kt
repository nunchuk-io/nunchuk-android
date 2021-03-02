package com.nunchuk.android.usecase

import com.nunchuk.android.type.ExportFormat

interface ExportTransactionHistoryUseCase {
    fun execute(walletId: String, filePath: String, format: ExportFormat = ExportFormat.CSV)
}