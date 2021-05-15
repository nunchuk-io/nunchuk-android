package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface UpdateTransactionMemoUseCase {
    suspend fun execute(walletId: String, txId: String, newMemo: String): Result<Boolean>
}

internal class UpdateTransactionMemoUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), UpdateTransactionMemoUseCase {

    override suspend fun execute(walletId: String, txId: String, newMemo: String) = exe {
        nunchukFacade.updateTransactionMemo(
            walletId = walletId,
            txId = txId,
            newMemo = newMemo
        )
    }

}