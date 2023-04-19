package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UploadCoinControlData @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: PremiumWalletRepository,
) : UseCase<UploadCoinControlData.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        return repository.uploadCoinControlData(parameters.walletId, parameters.data)
    }

    data class Param(val walletId: String, val data: String, val isForce: Boolean)
}