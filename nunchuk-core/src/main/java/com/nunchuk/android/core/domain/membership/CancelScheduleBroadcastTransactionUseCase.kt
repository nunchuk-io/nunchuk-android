package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CancelScheduleBroadcastTransactionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
) : UseCase<CancelScheduleBroadcastTransactionUseCase.Param, ServerTransaction>(dispatcher) {
    override suspend fun execute(parameters: Param): ServerTransaction {
        return userWalletRepository.deleteScheduleTransaction(
            parameters.walletId,
            parameters.transactionId,
        )
    }

    class Param(val walletId: String, val transactionId: String)
}