package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetKeyPolicyUserDataUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
) : UseCase<GetKeyPolicyUserDataUseCase.Param, String>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): String {
        return userWalletRepository.generateUpdateServerKey(
            parameters.walletId,
            parameters.keyPolicy
        )
    }

    class Param(val walletId: String, val keyPolicy: KeyPolicy)
}