package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DeleteKeyInWalletUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
) : UseCase<DeleteKeyInWalletUseCase.Params, Unit>(dispatcher) {
    override suspend fun execute(parameters: Params) {
        return userWalletRepository.deleteKeyForReplacedWallet(
            groupId = parameters.groupId,
            walletId = parameters.walletId
        )
    }

    data class Params(val walletId: String, val groupId: String)
}