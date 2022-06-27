package com.nunchuk.android.settings.developer

import com.nunchuk.android.core.domain.data.DeveloperSetting

data class DeveloperSettingState(
    val developerSetting: DeveloperSetting = DeveloperSetting()
)

sealed class DeveloperSettingEvent {
    data class UpdateSuccessEvent(val developerSetting: DeveloperSetting) : DeveloperSettingEvent()
}