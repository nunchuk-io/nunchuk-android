package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetTransactionHistoryUseCase {
    suspend fun execute(walletId: String, count: Int = 1000, skip: Int = 0): Result<List<Transaction>>
}

internal class GetTransactionHistoryUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), GetTransactionHistoryUseCase {

    override suspend fun execute(walletId: String, count: Int, skip: Int) = exe {
        nunchukFacade.getTransactionHistory(walletId = walletId, count = count, skip = skip)
    }
}