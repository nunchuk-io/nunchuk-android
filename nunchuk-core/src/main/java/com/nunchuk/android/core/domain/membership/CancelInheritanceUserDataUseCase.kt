package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CancelInheritanceUserDataUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
) : UseCase<CancelInheritanceUserDataUseCase.Param, String>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): String {
        return userWalletRepository.generateCancelInheritanceUserData(walletId = parameters.walletId)
    }

    class Param(val walletId: String)
}