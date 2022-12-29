package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.SeverWallet
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateServerWalletUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<CreateServerWalletUseCase.Params, SeverWallet>(dispatcher) {
    override suspend fun execute(parameters: Params): SeverWallet {
        return userWalletsRepository.createServerWallet(parameters.wallet, parameters.serverKeyId, parameters.plan)
    }

    data class Params(val wallet: Wallet, val serverKeyId: String, val plan: MembershipPlan)
}