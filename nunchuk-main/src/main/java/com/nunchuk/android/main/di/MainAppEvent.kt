package com.nunchuk.android.main.di

import com.nunchuk.android.core.api.SyncStateMatrixResponse
import com.nunchuk.android.type.ConnectionStatus
import okhttp3.ResponseBody

internal sealed class MainAppEvent {
    data class DownloadFileSyncSucceed(val jsonInfo: String, val responseBody: ResponseBody) : MainAppEvent()
    data class UploadFileSyncSucceed(val fileJsonInfo: String, val fileUri: String) : MainAppEvent()
    data class SyncInitMatrixStateSucceed(val response: SyncStateMatrixResponse) : MainAppEvent()
    data class GetConnectionStatusSuccessEvent(
        val connectionStatus: ConnectionStatus
    ) : MainAppEvent()
}