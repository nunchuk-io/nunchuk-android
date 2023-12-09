package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DenyInheritanceRequestPlanningUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<DenyInheritanceRequestPlanningUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        userWalletsRepository.denyInheritanceRequestPlanning(
            requestId = parameters.requestId,
            groupId = parameters.groupId,
            walletId = parameters.walletId,
        )
    }

    class Param(
        val requestId: String,
        val groupId: String,
        val walletId: String,
    )
}