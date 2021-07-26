package com.nunchuk.android.messages.util

import com.nunchuk.android.messages.components.detail.Message
import com.nunchuk.android.messages.components.detail.MessageType
import com.nunchuk.android.messages.components.detail.RoomInfo
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent

fun TimelineEvent.isMessageEvent() = root.getClearType() == EventType.MESSAGE

fun TimelineEvent.lastMessage(): CharSequence {
    val senderName = senderInfo.disambiguatedDisplayName
    val lastMessage = getTextEditableContent() ?: getLastMessageContent()?.body
    return "$senderName: $lastMessage"
}

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
        RoomInfo(roomSummary.getRoomName(currentName), roomSummary.joinedMembersCount ?: 0)
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

fun TimelineEvent.toMessage(chatId: String) = Message(
    sender = senderInfo.displayName ?: "Guest",
    content = root.getClearContent().toModel<MessageContent>()?.body.orEmpty(),
    type = if (chatId == senderInfo.userId) MessageType.CHAT_MINE.index else MessageType.CHAT_PARTNER.index
)
