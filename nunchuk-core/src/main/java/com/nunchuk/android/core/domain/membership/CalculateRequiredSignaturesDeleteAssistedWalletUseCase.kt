package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CalculateRequiredSignaturesDeleteAssistedWalletUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<String, CalculateRequiredSignatures>(dispatcher) {
    override suspend fun execute(parameters: String): CalculateRequiredSignatures {
        return userWalletsRepository.calculateRequiredSignaturesDeleteAssistedWallet(
            walletId = parameters,
        )
    }
}