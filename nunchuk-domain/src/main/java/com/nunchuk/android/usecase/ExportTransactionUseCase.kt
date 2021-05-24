package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface ExportTransactionUseCase {
    suspend fun execute(walletId: String, txId: String, filePath: String): Result<Boolean>
}

internal class ExportTransactionUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), ExportTransactionUseCase {

    override suspend fun execute(walletId: String, txId: String, filePath: String) = exe {
        nunchukFacade.exportTransaction(walletId = walletId, txId = txId, filePath = filePath)
    }

}