package com.nunchuk.android.messages.util

import android.util.Log
import com.google.gson.Gson
import com.nunchuk.android.messages.components.detail.Message
import com.nunchuk.android.messages.components.detail.MessageType
import com.nunchuk.android.messages.components.detail.NotificationMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomNameContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent

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
                time = time(),
                type = chatType(chatId)
            )
        }
        isMessageEvent() -> {
            Message(
                sender = senderSafe(),
                content = root.getClearContent().toModel<MessageContent>()?.body.orEmpty(),
                root.sendState,
                time = time(),
                type = chatType(chatId)
            )
        }
        else -> {
            NotificationMessage(
                sender = senderSafe(),
                content = root.content.toModel<RoomMemberContent>()?.displayName ?: senderSafe(),
                time = time(),
                timelineEvent = this
            )
        }
    }
}

private fun TimelineEvent.chatType(chatId: String) = if (chatId == senderInfo.userId) {
    MessageType.TYPE_CHAT_MINE.index
} else {
    MessageType.TYPE_CHAT_PARTNER.index
}

private fun TimelineEvent.senderSafe() = senderInfo.displayNameOrId()

private fun SenderInfo?.displayNameOrId(): String = this?.displayName ?: this?.userId ?: "Guest"

fun TimelineEvent.membership(): Membership {
    val content = root.content.toModel<RoomMemberContent>()
    return content?.membership ?: Membership.NONE
}

fun TimelineEvent.nameChange(): String? {
    return root.content.toModel<RoomNameContent>()?.name
}

private fun TimelineEvent.time() = root.originServerTs ?: 0