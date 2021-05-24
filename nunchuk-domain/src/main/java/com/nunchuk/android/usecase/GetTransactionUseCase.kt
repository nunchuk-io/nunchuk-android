package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetTransactionUseCase {
    suspend fun execute(walletId: String, txId: String): Result<Transaction>
}

internal class GetTransactionUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), GetTransactionUseCase {
    override suspend fun execute(walletId: String, txId: String) = exe {
        nunchukFacade.getTransaction(walletId = walletId, txId = txId)
    }

}