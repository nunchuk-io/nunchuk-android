package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetTransactionHistoryUseCase {
    fun execute(walletId: String, count: Int = 1000, skip: Int = 0): Flow<List<Transaction>>
}

internal class GetTransactionHistoryUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetTransactionHistoryUseCase {

    override fun execute(walletId: String, count: Int, skip: Int) = flow {
        val chainTip = nativeSdk.getChainTip()
        val transactions = nativeSdk.getTransactionHistory(
            walletId = walletId,
            count = count,
            skip = skip
        ).map { it.copy(height = it.getConfirmations(chainTip)) }
        emit(transactions)
    }.catch {
        CrashlyticsReporter.recordException(it)
        emit(emptyList())
    }
}

fun Transaction.getConfirmations(chainTip: Int) = if (chainTip > 0 && height > 0 && chainTip >= height) {
    (chainTip - height + 1)
} else {
    0
}
