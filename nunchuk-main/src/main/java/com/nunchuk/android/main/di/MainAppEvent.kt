package com.nunchuk.android.main.di

import com.nunchuk.android.core.data.model.AppUpdateResponse
import com.nunchuk.android.type.ConnectionStatus

internal sealed class MainAppEvent {
    data class GetConnectionStatusSuccessEvent(
        val connectionStatus: ConnectionStatus
    ) : MainAppEvent()

    object SyncCompleted : MainAppEvent()

    object ConsumeSyncEventCompleted : MainAppEvent()

    data class UpdateAppRecommendEvent(
        val data: AppUpdateResponse
    ) : MainAppEvent()
}