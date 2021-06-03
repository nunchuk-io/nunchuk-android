package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetTransactionUseCase {
    suspend fun execute(walletId: String, txId: String): Result<Transaction>
}

internal class GetTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetTransactionUseCase {
    override suspend fun execute(walletId: String, txId: String) = exe {
        nativeSdk.getTransaction(walletId = walletId, txId = txId)
    }

}