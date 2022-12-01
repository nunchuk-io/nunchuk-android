package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CalculateRequiredSignaturesUpdateKeyPolicyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<CalculateRequiredSignaturesUpdateKeyPolicyUseCase.Param, CalculateRequiredSignatures>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): CalculateRequiredSignatures {
        return userWalletsRepository.calculateRequiredSignaturesUpdateKeyPolicy(
            walletId = parameters.walletId,
            xfp = parameters.xfp,
            keyPolicy = parameters.keyPolicy
        )
    }

    class Param(val walletId: String, val xfp: String, val keyPolicy: KeyPolicy)
}