package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class VerifyConfirmationCodeUseCase @Inject constructor(
    private val repository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<VerifyConfirmationCodeUseCase.Param, String>(ioDispatcher) {

    override suspend fun execute(parameters: Param): String {
        return repository.verifyConfirmationCode(
            code = parameters.code,
            codeId = parameters.codeId
        )
    }

    class Param(val code: String, val codeId: String)
}