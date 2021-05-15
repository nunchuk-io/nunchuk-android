package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface BroadcastTransactionUseCase {
    suspend fun execute(walletId: String, txId: String): Result<Transaction>
}

internal class BroadcastTransactionUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), BroadcastTransactionUseCase {

    override suspend fun execute(walletId: String, txId: String) = exe {
        nunchukFacade.broadcastTransaction(walletId = walletId, txId = txId)
    }

}