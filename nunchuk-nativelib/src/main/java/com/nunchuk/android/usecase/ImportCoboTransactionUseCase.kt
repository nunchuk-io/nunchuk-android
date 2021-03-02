package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction

interface ImportCoboTransactionUseCase {
    fun execute(walletId: String, qrData: List<String>): Transaction
}