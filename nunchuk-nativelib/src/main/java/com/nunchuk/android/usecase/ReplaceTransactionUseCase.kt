package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction

interface ReplaceTransactionUseCase {
    fun execute(walletId: String, txId: String, newFeeRate: Amount): Transaction
}