package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateOrUpdateWalletPinUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val settingRepository: SettingRepository,
    private val androidNativeSdk: NunchukNativeSdk,
) : UseCase<String, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: String) {
        if (parameters.isNotBlank()) {
            settingRepository.setWalletPin(androidNativeSdk.hashSHA256(parameters))
        } else {
            settingRepository.setWalletPin("")
        }
    }
}