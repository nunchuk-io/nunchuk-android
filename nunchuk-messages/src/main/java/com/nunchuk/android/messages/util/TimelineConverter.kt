package com.nunchuk.android.messages.util

import com.google.gson.Gson
import com.nunchuk.android.messages.components.detail.Message
import com.nunchuk.android.messages.components.detail.MessageType
import com.nunchuk.android.messages.components.detail.NotificationMessage
import com.nunchuk.android.messages.components.detail.NunchukWalletMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber

fun List<TimelineEvent>.toMessages(chatId: String) = sortedBy { it.root.ageLocalTs }.mapNotNull { it.toMessageSafe(chatId) }

fun TimelineEvent.toMessageSafe(chatId: String): Message? = try {
    toMessage(chatId)
} catch (e: Exception) {
    Timber.e(e)
    null
}

fun TimelineEvent.toMessage(chatId: String): Message {
    Timber.d(TAG, "$this")
    return when {
        isNunchukWalletEvent() -> {
            val content = root.content?.toMap().orEmpty()
            val msgType = WalletEventType.of(content[KEY] as String)
            NunchukWalletMessage(
                sender = senderSafe(),
                content = Gson().toJson(root.getClearContent()),
                time = time(),
                timelineEvent = this,
                eventType = root.type!!,
                msgType = WalletEventType.of(content[KEY] as String),
                type = if (msgType == WalletEventType.INIT) MessageType.TYPE_NUNCHUK_CARD.index else MessageType.TYPE_NUNCHUK_NOTIFICATION.index,
                isOwner = chatId == senderInfo.userId
            )
        }
        isNunchukTransactionEvent() -> {
            val content = root.content?.toMap().orEmpty()
            NunchukWalletMessage(
                sender = senderSafe(),
                content = Gson().toJson(root.getClearContent()),
                time = time(),
                timelineEvent = this,
                eventType = root.type!!,
                msgType = WalletEventType.of(content[KEY] as String)
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
                content = Gson().toJson(root.getClearContent()),
                time = time(),
                timelineEvent = this
            )
        }
    }
}
