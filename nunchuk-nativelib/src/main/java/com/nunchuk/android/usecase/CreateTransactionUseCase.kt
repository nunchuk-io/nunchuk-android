package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface CreateTransactionUseCase {
    suspend fun execute(
        walletId: String,
        outputs: Map<String, Amount>,
        memo: String = "",
        inputs: List<UnspentOutput> = emptyList(),
        feeRate: Amount = Amount(-1),
        subtractFeeFromAmount: Boolean = false
    ): Result<Transaction>
}

internal class CreateTransactionUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), CreateTransactionUseCase {
    override suspend fun execute(
        walletId: String,
        outputs: Map<String, Amount>,
        memo: String,
        inputs: List<UnspentOutput>,
        feeRate: Amount,
        subtractFeeFromAmount: Boolean
    ) = exe {
        nunchukFacade.createTransactionUseCase(
            walletId = walletId,
            outputs = outputs,
            memo = memo,
            inputs = inputs,
            feeRate = feeRate,
            subtractFeeFromAmount = subtractFeeFromAmount
        )
    }

}