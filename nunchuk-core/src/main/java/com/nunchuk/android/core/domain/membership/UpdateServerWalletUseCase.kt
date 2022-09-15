package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SeverWallet
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateServerWalletUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<UpdateServerWalletUseCase.Params, SeverWallet>(dispatcher) {
    override suspend fun execute(parameters: Params): SeverWallet {
        return userWalletsRepository.updateServerWallet(
            walletLocalId = parameters.walletId,
            name = parameters.name
        )
    }

    data class Params(val walletId: String, val name: String)
}