package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateServerKeysUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<CreateServerKeysUseCase.Param, KeyPolicy>(dispatcher) {
    override suspend fun execute(parameters: Param): KeyPolicy {
        return userWalletsRepository.createServerKeys(
            parameters.name, parameters.keyPolicy, parameters.plan
        )
    }

    data class Param(
        val name: String,
        val keyPolicy: KeyPolicy,
        val plan: MembershipPlan,
    )
}