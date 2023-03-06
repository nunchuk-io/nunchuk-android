package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ReuseKeyWalletUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: PremiumWalletRepository,
) : UseCase<ReuseKeyWalletUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        repository.reuseKeyWallet(parameters.walletId, parameters.plan)
    }

    data class Param(val walletId: String, val plan: MembershipPlan)
}