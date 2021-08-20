package com.nunchuk.android.messages.util

import com.nunchuk.android.messages.components.detail.RoomInfo
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

fun List<RoomSummary>.sortByLastMessage(): List<RoomSummary> {
    return sortedByDescending { it.latestPreviewableEvent?.root?.originServerTs }
}

fun Room.isDirectRoom(): Boolean {
    val queryParams = RoomMemberQueryParams.Builder().build()
    val roomMembers: List<RoomMemberSummary> = getRoomMembers(queryParams)
    return roomSummary()?.isDirect.orFalse() || roomMembers.size == 2
}

fun Room.getRoomInfo(currentName: String): RoomInfo {
    val roomSummary: RoomSummary? = roomSummary()
    return if (roomSummary != null) {
        RoomInfo(roomSummary.getRoomName(currentName), roomSummary.getMembersCount())
    } else {
        RoomInfo.empty()
    }
}

fun Room.getRoomMemberList(): List<RoomMemberSummary> {
    val queryParams = RoomMemberQueryParams.Builder().build()
    return getRoomMembers(queryParams)
}

fun RoomSummary.getRoomName(currentName: String): String {
    val split = displayName.split(",")
    return if (split.size == 2) {
        split.firstOrNull { it != currentName }.orEmpty()
    } else {
        displayName
    }
}

fun List<TimelineEvent>.toMessages(chatId: String) = sortedBy { it.root.ageLocalTs }.map { it.toMessage(chatId) }

fun RoomSummary.getMembersCount() = otherMemberIds.size + 1
