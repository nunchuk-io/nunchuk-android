package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction

interface GetTransactionHistoryUseCase {
    fun execute(wallet_id: String, count: Int, skip: Int): List<Transaction>
}