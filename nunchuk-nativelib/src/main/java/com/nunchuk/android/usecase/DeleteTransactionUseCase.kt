package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface DeleteTransactionUseCase {
    suspend fun execute(walletId: String, txId: String): Result<Boolean>
}

internal class DeleteTransactionUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), DeleteTransactionUseCase {

    override suspend fun execute(walletId: String, txId: String) = exe {
        nunchukFacade.deleteTransaction(walletId = walletId, txId = txId)
    }
}