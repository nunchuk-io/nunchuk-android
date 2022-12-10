package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ScheduleBroadcastTransactionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
) : UseCase<ScheduleBroadcastTransactionUseCase.Param, Long>(
    dispatcher
) {
    override suspend fun execute(parameters: Param) : Long {
       return userWalletRepository.scheduleTransaction(
            parameters.walletId,
            parameters.transactionId,
            parameters.scheduleTime
        )
    }

    class Param(val walletId: String, val transactionId: String, val scheduleTime: Long)
}