package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetServerKeysUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<String, KeyPolicy>(dispatcher) {
    override suspend fun execute(parameters: String): KeyPolicy {
        return userWalletsRepository.getServerKey(parameters)
    }
}