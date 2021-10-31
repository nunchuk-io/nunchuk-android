package com.nunchuk.android.settings.network

import com.nunchuk.android.model.AppSettings

data class NetworkSettingState (
    val appSetting: AppSettings = AppSettings()
)

sealed class NetworkSettingEvent {
    data class UpdateSettingSuccessEvent(val appSetting: AppSettings): NetworkSettingEvent()
}