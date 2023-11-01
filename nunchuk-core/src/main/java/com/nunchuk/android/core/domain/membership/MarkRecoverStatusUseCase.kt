package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class MarkRecoverStatusUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<MarkRecoverStatusUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        return userWalletsRepository.markKeyAsRecovered(
            xfp = parameters.xfp,
            status = parameters.status
        )
    }

    class Param(
        val xfp: String,
        val status: String
    )
}