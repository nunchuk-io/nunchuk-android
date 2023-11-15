package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RequestConfirmationCodeUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<RequestConfirmationCodeUseCase.Param, Pair<String, String>>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Pair<String, String> {
        return repository.requestConfirmationCode(
            action = parameters.action,
            userData = parameters.userData
        )
    }

    class Param(val action: String, val userData: String)
}