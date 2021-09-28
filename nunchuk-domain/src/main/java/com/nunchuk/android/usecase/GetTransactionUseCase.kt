package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TransactionExt
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetTransactionUseCase {
    fun execute(walletId: String, txId: String): Flow<Transaction>
}

internal class GetTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetTransactionUseCase {

    override fun execute(walletId: String, txId: String) = flow {
        emit(nativeSdk.getTransaction(walletId = walletId, txId = txId))
    }

}

interface GetTransactionsUseCase {
    fun execute(eventIds: List<String>): Flow<List<TransactionExt>>
}

internal class GetTransactionsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetTransactionsUseCase {
    override fun execute(eventIds: List<String>): Flow<List<TransactionExt>> = flow {
        val transactions = eventIds.mapNotNull {
            try {
                val roomTx = nativeSdk.getRoomTransaction(it)
                val tx = nativeSdk.getTransaction(roomTx.walletId, txId = roomTx.txId)
                TransactionExt(walletId = roomTx.walletId, it, tx)
            } catch (t: Throwable) {
                CrashlyticsReporter.recordException(t)
                null
            }
        }
        emit(transactions)
    }
}

