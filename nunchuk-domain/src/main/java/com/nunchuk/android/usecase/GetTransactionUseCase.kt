package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TransactionExt
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
    fun execute(walletId: String, eventIds: List<Pair<String, Boolean>>): Flow<List<TransactionExt>>
}

internal class GetTransactionsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetTransactionsUseCase {

    override fun execute(walletId: String, eventIds: List<Pair<String, Boolean>>) = flow {
        val transactions = eventIds.mapNotNull {
            val initEventId = it.first
            val isReceive = it.second
            getTransaction(walletId = walletId, initEventId = initEventId, isReceive = isReceive)
        }
        emit(transactions)
    }.catch { emit(emptyList()) }

    private fun getTransaction(walletId: String, initEventId: String, isReceive: Boolean): TransactionExt? {
        try {
            val txId = if (isReceive) {
                nativeSdk.getTransactionId(initEventId)
            } else {
                nativeSdk.getRoomTransaction(initEventId).txId
            }
            if (txId.isEmpty()) return null

            val tx = nativeSdk.getTransaction(walletId, txId = txId)
            return TransactionExt(walletId = walletId, initEventId, tx)
        } catch (t: Throwable) {
            CrashlyticsReporter.recordException(t)
            return null
        }
    }
}

