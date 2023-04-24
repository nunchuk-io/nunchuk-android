package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SyncCoinControlData @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val repository: PremiumWalletRepository,
) : UseCase<String, Unit>(dispatcher) {
    override suspend fun execute(parameters: String) {
        val serverData = repository.getCoinControlData(parameters)
        val result = nunchukNativeSdk.importCoinControlData(parameters, serverData, false)
        if (!result) {
            val deviceData = nunchukNativeSdk.exportCoinControlData(parameters)
            repository.uploadCoinControlData(parameters, deviceData)
        }
    }
}