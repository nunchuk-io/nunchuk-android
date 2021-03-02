package com.nunchuk.android.usecase

interface ExportCoboTransactionUseCase {
    fun execute(walletId: String, txId: String): List<String>
}