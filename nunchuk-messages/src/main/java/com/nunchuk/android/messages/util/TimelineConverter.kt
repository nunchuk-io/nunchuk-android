package com.nunchuk.android.messages.util

import com.google.gson.Gson
import com.nunchuk.android.messages.components.detail.Message
import com.nunchuk.android.messages.components.detail.NotificationMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber

fun List<TimelineEvent>.toMessages(chatId: String) = sortedBy { it.root.ageLocalTs }.map { it.toMessage(chatId) }

fun TimelineEvent.toMessage(chatId: String): Message {
    Timber.d(TAG, "$this")
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
