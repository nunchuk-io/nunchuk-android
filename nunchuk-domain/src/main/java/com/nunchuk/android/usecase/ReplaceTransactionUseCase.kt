package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface ReplaceTransactionUseCase {
    suspend fun execute(walletId: String, txId: String, newFeeRate: Amount): Result<Transaction>
}

internal class ReplaceTransactionUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), ReplaceTransactionUseCase {

    override suspend fun execute(walletId: String, txId: String, newFeeRate: Amount) = exe {
        nunchukFacade.replaceTransaction(
            walletId = walletId,
            txId = txId,
            newFeeRate = newFeeRate
        )
    }

}