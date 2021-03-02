package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.UnspentOutput

interface CreateTransactionUseCase {
    fun execute(
            walletId: String,
            outputs: Map<String, Amount>,
            memo: String = "",
            inputs: List<UnspentOutput> = emptyList(),
            feeRate: Amount = Amount(-1),
            subtractFeeFromAmount: Boolean = false
    ): Transaction
}