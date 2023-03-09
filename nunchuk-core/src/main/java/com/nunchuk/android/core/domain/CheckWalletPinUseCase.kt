package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CheckWalletPinUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val settingRepository: SettingRepository,
    private val androidNativeSdk: NunchukNativeSdk,
) : UseCase<String, Boolean>(ioDispatcher) {

    override suspend fun execute(parameters: String): Boolean {
        val currentPin = settingRepository.walletPin.first()
        val hashedValue = androidNativeSdk.hashSHA256(parameters)
        return currentPin == hashedValue
    }
}