package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface DeleteTransactionUseCase {
    suspend fun execute(walletId: String, txId: String): Result<Boolean>
}

internal class DeleteTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), DeleteTransactionUseCase {

    override suspend fun execute(walletId: String, txId: String) = exe {
        nativeSdk.deleteTransaction(walletId = walletId, txId = txId)
    }
}