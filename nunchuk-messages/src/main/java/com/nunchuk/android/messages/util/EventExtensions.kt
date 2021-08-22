package com.nunchuk.android.messages.util

import android.util.Log
import com.google.gson.Gson
import com.nunchuk.android.messages.components.detail.Message
import com.nunchuk.android.messages.components.detail.MessageType
import com.nunchuk.android.messages.components.detail.NotificationMessage
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.isTextMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent

fun TimelineEvent.isDisplayable() = isMessageEvent() || isNunchukEvent() || isNotificationType()

fun TimelineEvent.isNotificationType() = root.getClearType() == EventType.STATE_ROOM_MEMBER

fun TimelineEvent.isMessageEvent() = root.isTextMessage()

fun TimelineEvent.isNunchukEvent() = root.type == "io.nunchuk.wallet"

fun TimelineEvent.lastMessage(): CharSequence {
    val senderName = senderInfo.disambiguatedDisplayName
    val lastMessage = getTextEditableContent() ?: getLastMessageContent()?.body
    return "$senderName: $lastMessage"
}

fun TimelineEvent.toMessage(chatId: String): Message {
    Log.d("TimelineEvent", "${this}")
    return when {
        isNunchukEvent() -> {
            Message(
                sender = senderSafe(),
                content = Gson().toJson(root.getClearContent()),
                type = chatType(chatId)
            )
        }
        isNotificationType() -> {
            NotificationMessage(
                sender = senderSafe(),
                content = root.content.toModel<RoomMemberContent>()?.displayName.orGuest(),
                type = MessageType.NOTIFICATION.index,
                membership = membership()
            )
        }
        else -> {
            Message(
                sender = senderSafe(),
                content = root.getClearContent().toModel<MessageContent>()?.body.orEmpty(),
                root.sendState,
                type = chatType(chatId)
            )
        }
    }
}

private fun TimelineEvent.chatType(chatId: String) = if (chatId == senderInfo.userId) {
    MessageType.CHAT_MINE.index
} else {
    MessageType.CHAT_PARTNER.index
}

private fun TimelineEvent.senderSafe() = senderInfo.displayName.orGuest()

private fun String?.orGuest() = this ?: "Guest"

private fun TimelineEvent.membership(): Membership {
    val content = root.content.toModel<RoomMemberContent>()
    return content?.membership ?: Membership.NONE
}