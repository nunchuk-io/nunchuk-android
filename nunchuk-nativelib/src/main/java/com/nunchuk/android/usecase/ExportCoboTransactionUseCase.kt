package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface ExportCoboTransactionUseCase {
    suspend fun execute(walletId: String, txId: String): Result<List<String>>
}

internal class ExportCoboTransactionUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), ExportCoboTransactionUseCase {

    override suspend fun execute(walletId: String, txId: String) = exe {
        nunchukFacade.exportCoboTransaction(walletId = walletId, txId = txId)
    }

}