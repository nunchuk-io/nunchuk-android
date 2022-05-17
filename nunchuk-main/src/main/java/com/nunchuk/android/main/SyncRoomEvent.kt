package com.nunchuk.android.main

import org.matrix.android.sdk.api.session.Session

internal sealed class SyncRoomEvent {
    data class FindSyncRoomSuccessEvent(val syncRoomId: String) : SyncRoomEvent()
    data class FindSyncRoomFailedEvent(val syncRoomSize: Int) : SyncRoomEvent()
    data class CreateSyncRoomSucceedEvent(val syncRoomId: String) : SyncRoomEvent()
    data class LoginMatrixSucceedEvent(val session: Session) : SyncRoomEvent()
}