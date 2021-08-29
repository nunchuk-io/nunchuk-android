package com.nunchuk.android.messages.util

import com.google.gson.Gson
import com.nunchuk.android.messages.components.detail.MessageType
import com.nunchuk.android.model.NunchukMatrixEvent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomNameContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent

internal const val TAG = "TimelineEvent"

fun TimelineEvent.lastMessage(): CharSequence {
    val senderName = senderInfo.disambiguatedDisplayName
    val lastMessage = getTextEditableContent() ?: getLastMessageContent()?.body
    return "$senderName: $lastMessage"
}


fun TimelineEvent.membership(): Membership {
    val content = root.content.toModel<RoomMemberContent>()
    return content?.membership ?: Membership.NONE
}

fun TimelineEvent.nameChange() = root.content.toModel<RoomNameContent>()?.name

fun TimelineEvent.toNunchukMatrixEvent() = NunchukMatrixEvent(
    eventId = root.eventId!!,
    type = root.type!!,
    content = Gson().toJson(root.content?.toMap().orEmpty()),
    roomId = roomId,
    sender = senderInfo.userId,
    time = root.originServerTs ?: 0L
)

fun TimelineEvent.time() = root.originServerTs ?: 0

fun TimelineEvent.chatType(chatId: String) = if (chatId == senderInfo.userId) {
    MessageType.TYPE_CHAT_MINE.index
} else {
    MessageType.TYPE_CHAT_PARTNER.index
}

fun TimelineEvent.senderSafe() = senderInfo.displayNameOrId()

fun SenderInfo?.displayNameOrId(): String = this?.displayName ?: this?.userId ?: "Guest"
