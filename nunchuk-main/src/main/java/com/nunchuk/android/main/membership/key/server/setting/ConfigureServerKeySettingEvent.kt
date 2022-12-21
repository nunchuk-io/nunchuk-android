package com.nunchuk.android.main.membership.key.server.setting

import com.nunchuk.android.model.KeyPolicy

sealed class ConfigureServerKeySettingEvent {
    object NoDelayInput : ConfigureServerKeySettingEvent()
    object DelaySigningInHourInvalid : ConfigureServerKeySettingEvent()
    data class ShowError(val message: String) : ConfigureServerKeySettingEvent()
    data class Loading(val isLoading: Boolean) : ConfigureServerKeySettingEvent()
    data class ConfigServerSuccess(val keyPolicy: KeyPolicy) : ConfigureServerKeySettingEvent()
}

data class ConfigureServerKeySettingState(
    val cosigningTextHours: String = "",
    val cosigningTextMinutes: String = "",
    val autoBroadcastSwitched: Boolean = true,
    val enableCoSigningSwitched: Boolean = false,
) {
    companion object {
        val Empty = ConfigureServerKeySettingState()
    }
}