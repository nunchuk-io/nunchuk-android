package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CalculateRequiredSignaturesLockdownUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<CalculateRequiredSignaturesLockdownUseCase.Param, CalculateRequiredSignatures>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): CalculateRequiredSignatures {
        return userWalletsRepository.calculateRequiredSignaturesLockdown(
            walletId = parameters.walletId,
            periodId = parameters.periodId
        )
    }

    class Param(val walletId: String, val periodId: String)
}