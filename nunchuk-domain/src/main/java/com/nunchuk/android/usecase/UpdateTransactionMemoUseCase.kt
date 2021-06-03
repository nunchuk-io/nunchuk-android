package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface UpdateTransactionMemoUseCase {
    suspend fun execute(walletId: String, txId: String, newMemo: String): Result<Boolean>
}

internal class UpdateTransactionMemoUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), UpdateTransactionMemoUseCase {

    override suspend fun execute(walletId: String, txId: String, newMemo: String) = exe {
        nativeSdk.updateTransactionMemo(
            walletId = walletId,
            txId = txId,
            newMemo = newMemo
        )
    }

}