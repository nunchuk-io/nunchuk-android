package com.nunchuk.android.usecase

interface DeleteTransactionUseCase {
    fun execute(walletId: String, txId: String): Boolean
}