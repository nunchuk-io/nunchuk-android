package com.nunchuk.android.usecase

interface ExportTransactionUseCase {
    fun execute(walletId: String, txId: String, filePath: String): Boolean
}