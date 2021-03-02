package com.nunchuk.android.usecase

interface UpdateTransactionMemoUseCase {
    fun execute(walletId: String, txId: String, newMemo: String): Boolean
}