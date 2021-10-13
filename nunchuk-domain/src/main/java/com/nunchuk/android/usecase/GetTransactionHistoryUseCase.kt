package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
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
        emit(nativeSdk.getTransactionHistory(walletId = walletId, count = count, skip = skip))
    }.catch { emit(emptyList()) }
}