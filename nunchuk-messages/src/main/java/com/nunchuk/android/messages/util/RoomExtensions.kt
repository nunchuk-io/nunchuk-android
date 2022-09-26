package com.nunchuk.android.messages.util

import com.nunchuk.android.messages.components.detail.RoomInfo
import com.nunchuk.android.messages.components.list.DIRECT_CHAT_MEMBERS_COUNT
import com.nunchuk.android.messages.components.list.getRoomName
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomSummary

fun Room.isDirectChat(): Boolean {
    val roomMembers = getRoomMemberList()
    return roomSummary()?.isDirect.orFalse() || roomMembers.size == DIRECT_CHAT_MEMBERS_COUNT
}

fun Room.getRoomInfo(currentName: String): RoomInfo {
    val roomMembers = getRoomMemberList()
    val roomSummary: RoomSummary? = roomSummary()
    return if (roomSummary != null) {
        RoomInfo(roomSummary.getRoomName(currentName), roomMembers.size)
    } else {
        RoomInfo.empty()
    }
}

fun Room.getRoomMemberList() = membershipService().getRoomMembers(roomMemberQueryParams())

private fun roomMemberQueryParams() = RoomMemberQueryParams.Builder().build()
