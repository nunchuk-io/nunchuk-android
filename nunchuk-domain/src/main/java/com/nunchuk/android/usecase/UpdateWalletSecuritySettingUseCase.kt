package com.nunchuk.android.usecase

import com.google.gson.Gson
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.repository.SettingRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateWalletSecuritySettingUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
    private val gson: Gson,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<WalletSecuritySetting, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: WalletSecuritySetting) {
        settingRepository.setWalletSecuritySetting(gson.toJson(parameters))
    }
}