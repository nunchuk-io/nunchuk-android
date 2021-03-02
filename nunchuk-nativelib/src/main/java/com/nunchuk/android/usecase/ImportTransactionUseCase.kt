package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction

interface ImportTransactionUseCase {
    fun execute(walletId: String, txId: String, filePath: String): Transaction
}