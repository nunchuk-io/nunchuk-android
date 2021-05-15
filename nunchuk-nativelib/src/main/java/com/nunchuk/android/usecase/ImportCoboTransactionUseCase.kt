package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface ImportCoboTransactionUseCase {
    suspend fun execute(walletId: String, qrData: List<String>): Result<Transaction>
}

internal class ImportCoboTransactionUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), ImportCoboTransactionUseCase {

    override suspend fun execute(walletId: String, qrData: List<String>) = exe {
        nunchukFacade.importCoboTransaction(walletId = walletId, qrData = qrData)
    }

}