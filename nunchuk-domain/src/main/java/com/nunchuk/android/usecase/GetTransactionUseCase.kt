package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetTransactionUseCase {
    suspend fun execute(walletId: String, txId: String): Flow<Transaction>
}

internal class GetTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetTransactionUseCase {

    override suspend fun execute(walletId: String, txId: String) = flow {
        emit(nativeSdk.getTransaction(walletId = walletId, txId = txId))
    }

}