package com.nunchuk.android.usecase

import com.nunchuk.android.model.TransactionExtended
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface GetTransactionsUseCase {
    fun execute(walletId: String, eventIds: List<Pair<String, Boolean>>): Flow<List<TransactionExtended>>
}

internal class GetTransactionsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetTransactionsUseCase {

    override fun execute(walletId: String, eventIds: List<Pair<String, Boolean>>) = flow {
        emit(eventIds.mapNotNull {
            val initEventId = it.first
            val isReceive = it.second
            getTransaction(walletId = walletId, initEventId = initEventId, isReceive = isReceive)
        })
    }.catch {
        CrashlyticsReporter.recordException(it)
        emit(emptyList())
    }.flowOn(Dispatchers.IO)

    private fun getTransaction(walletId: String, initEventId: String, isReceive: Boolean): TransactionExtended? {
        try {
            val txId = if (isReceive) {
                nativeSdk.getTransactionId(initEventId)
            } else {
                nativeSdk.getRoomTransaction(initEventId).txId
            }
            if (txId.isEmpty()) return null
            val chainTip = nativeSdk.getChainTip()
            val tx = nativeSdk.getTransaction(walletId, txId = txId)
            return TransactionExtended(walletId = walletId, initEventId, tx.copy(height = tx.getConfirmations(chainTip)))
        } catch (t: Throwable) {
            CrashlyticsReporter.recordException(t)
            return null
        }
    }
}