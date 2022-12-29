package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.LockdownPeriod
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetLockdownPeriodUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
) : UseCase<Unit, List<LockdownPeriod>>(
    dispatcher
) {
    override suspend fun execute(parameters: Unit): List<LockdownPeriod> {
        return userWalletRepository.getLockdownPeriod()
    }
}