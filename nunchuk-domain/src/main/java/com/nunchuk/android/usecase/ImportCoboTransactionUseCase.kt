package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface ImportCoboTransactionUseCase {
    suspend fun execute(walletId: String, qrData: List<String>): Result<Transaction>
}

internal class ImportCoboTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), ImportCoboTransactionUseCase {

    override suspend fun execute(walletId: String, qrData: List<String>) = exe {
        nativeSdk.importCoboTransaction(walletId = walletId, qrData = qrData)
    }

}