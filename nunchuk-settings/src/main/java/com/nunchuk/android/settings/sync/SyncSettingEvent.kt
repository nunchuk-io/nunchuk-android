package com.nunchuk.android.settings.sync

import com.nunchuk.android.core.domain.data.SyncSetting

data class SyncSettingState(
    val syncSetting: SyncSetting = SyncSetting()
)

sealed class SyncSettingEvent {
    data class UpdateSyncSettingSuccessEvent(val enable: Boolean) : SyncSettingEvent()
}

