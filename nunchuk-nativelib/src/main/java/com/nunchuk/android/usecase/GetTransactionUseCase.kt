package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction

interface GetTransactionUseCase {
    fun execute(walletId: String, txId: String): Transaction
}