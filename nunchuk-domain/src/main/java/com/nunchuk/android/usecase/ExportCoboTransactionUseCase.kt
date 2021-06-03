package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface ExportCoboTransactionUseCase {
    suspend fun execute(walletId: String, txId: String): Result<List<String>>
}

internal class ExportCoboTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), ExportCoboTransactionUseCase {

    override suspend fun execute(walletId: String, txId: String) = exe {
        nativeSdk.exportCoboTransaction(walletId = walletId, txId = txId)
    }

}