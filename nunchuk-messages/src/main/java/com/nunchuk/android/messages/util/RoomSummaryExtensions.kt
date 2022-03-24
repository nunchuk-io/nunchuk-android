package com.nunchuk.android.messages.util

import com.nunchuk.android.messages.BuildConfig
import org.matrix.android.sdk.api.session.room.model.RoomSummary

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

fun RoomSummary.shouldShow() = BuildConfig.DEBUG || (!isServerNotices() && !isSyncRoom())

fun RoomSummary.isServerNotices() = name == SERVER_NOTICES

fun RoomSummary.isSyncRoom() = tags.isNotEmpty() && tags.any { it.name == STATE_NUNCHUK_SYNC }

const val SERVER_NOTICES = "Server Notices"

const val DIRECT_CHAT_MEMBERS_COUNT = 2
