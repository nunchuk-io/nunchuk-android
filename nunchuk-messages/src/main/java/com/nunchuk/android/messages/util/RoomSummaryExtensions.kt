package com.nunchuk.android.messages.util

import org.matrix.android.sdk.api.session.room.model.RoomSummary

fun List<RoomSummary>.sortByLastMessage(): List<RoomSummary> {
    return sortedByDescending { it.latestPreviewableEvent?.root?.originServerTs }
}

fun RoomSummary.getRoomName(currentName: String): String {
    val split = displayName.split(",")
    return if (split.size == DIRECT_CHAT_MEMBERS_COUNT) {
        split.firstOrNull { it != currentName } ?: currentName
    } else {
        displayName
    }
}

fun RoomSummary.getMembersCount() = otherMemberIds.size + 1

fun RoomSummary.isDirectChat() = isDirect || getMembersCount() <= DIRECT_CHAT_MEMBERS_COUNT

const val DIRECT_CHAT_MEMBERS_COUNT = 2
