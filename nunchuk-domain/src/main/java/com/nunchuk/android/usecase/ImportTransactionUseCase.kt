package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface ImportTransactionUseCase {
    suspend fun execute(walletId: String, filePath: String): Result<Transaction>
}

internal class ImportTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), ImportTransactionUseCase {

    override suspend fun execute(walletId: String, filePath: String) = exe {
        nativeSdk.importTransaction(
            walletId = walletId,
            filePath = filePath
        )
    }
}