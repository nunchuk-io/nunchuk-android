package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class FinalizeDummyTransactionUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
) : UseCase<FinalizeDummyTransactionUseCase.Params, Unit>(dispatcher) {
    override suspend fun execute(parameters: Params) {
        return userWalletRepository.finalizeDummyTransaction(
            groupId = parameters.groupId,
            walletId = parameters.walletId,
            dummyTransactionId = parameters.dummyTransactionId,
        )
    }

    data class Params(
        val groupId: String,
        val walletId: String,
        val dummyTransactionId: String,
    )
}