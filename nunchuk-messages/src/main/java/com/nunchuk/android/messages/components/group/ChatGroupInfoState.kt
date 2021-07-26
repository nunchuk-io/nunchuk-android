package com.nunchuk.android.messages.components.group

import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class ChatGroupInfoState(val summary: RoomSummary? = null, val roomMembers: List<RoomMemberSummary> = emptyList())

sealed class ChatGroupInfoEvent {
    object RoomNotFoundEvent : ChatGroupInfoEvent()
    data class UpdateRoomNameError(val message: String) : ChatGroupInfoEvent()
    data class UpdateRoomNameSuccess(val name: String) : ChatGroupInfoEvent()
}