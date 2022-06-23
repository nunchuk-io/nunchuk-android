package com.nunchuk.android.settings.unit

import com.nunchuk.android.core.domain.data.DisplayUnitSetting


data class DisplayUnitState(
    val displayUnitSetting: DisplayUnitSetting = DisplayUnitSetting()
)

sealed class DisplayUnitEvent {
    data class UpdateDisplayUnitSettingSuccessEvent(val displayUnitSetting: DisplayUnitSetting) : DisplayUnitEvent()
}