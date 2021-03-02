package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction

interface BroadcastTransactionUseCase {
    fun execute(walletId: String, txId: String): Transaction
}