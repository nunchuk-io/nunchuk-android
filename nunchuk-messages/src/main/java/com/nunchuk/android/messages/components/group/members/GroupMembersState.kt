package com.nunchuk.android.messages.components.group.members

import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary

data class GroupMembersState(
    val roomMembers: List<RoomMemberSummary> = emptyList()
)

sealed class GroupMembersEvent {
    object RoomNotFoundEvent : GroupMembersEvent()
}
