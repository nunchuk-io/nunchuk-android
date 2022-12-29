package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CalculateRequiredSignaturesInheritanceUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<CalculateRequiredSignaturesInheritanceUseCase.Param, CalculateRequiredSignatures>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): CalculateRequiredSignatures {
        return userWalletsRepository.calculateRequiredSignaturesInheritance(
            walletId = parameters.walletId,
            note = parameters.note,
            notificationEmails = parameters.notificationEmails,
            notifyToday = parameters.notifyToday,
            activationTimeMilis = parameters.activationTimeMilis
        )
    }

    class Param(
        val note: String,
        val notificationEmails: List<String>,
        val notifyToday: Boolean,
        val activationTimeMilis: Long,
        val walletId: String
    )
}