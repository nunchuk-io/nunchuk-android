package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RecoverKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<RecoverKeyUseCase.Param, BackupKey>(dispatcher) {
    override suspend fun execute(parameters: Param): BackupKey {
        return userWalletsRepository.recoverKey(
            xfp = parameters.xfp,
            verifyToken = parameters.verifyToken,
            securityQuestionToken = parameters.securityQuestionToken,
        )
    }

    class Param(
        val xfp: String,
        val verifyToken: String = "",
        val securityQuestionToken: String = "",
    )
}