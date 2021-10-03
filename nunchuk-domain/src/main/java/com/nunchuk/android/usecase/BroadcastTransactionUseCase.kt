package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface BroadcastTransactionUseCase {
    fun execute(walletId: String, txId: String): Flow<Transaction>
}

internal class BroadcastTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BroadcastTransactionUseCase {

    override fun execute(walletId: String, txId: String) = flow {
        emit(nativeSdk.broadcastTransaction(walletId = walletId, txId = txId))
    }.catch { CrashlyticsReporter.recordException(it) }

}
