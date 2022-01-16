package com.nunchuk.android.main.di

import com.nunchuk.android.type.ConnectionStatus
import okhttp3.ResponseBody

internal sealed class MainAppEvent {
    data class DownloadFileSyncSucceed(val jsonInfo: String, val responseBody: ResponseBody) : MainAppEvent()
    data class GetConnectionStatusSuccessEvent(
        val connectionStatus: ConnectionStatus
    ) : MainAppEvent()
}