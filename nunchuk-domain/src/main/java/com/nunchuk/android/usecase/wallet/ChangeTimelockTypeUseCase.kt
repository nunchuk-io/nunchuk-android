package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ChangeTimelockTypeUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<ChangeTimelockTypeUseCase.Param, Unit>(dispatcher) {

    override suspend fun execute(parameters: Param) {
        repository.changeTimelockType(parameters.groupId, parameters.walletId)
    }

    data class Param(
        val groupId: String?,
        val walletId: String
    )
}

