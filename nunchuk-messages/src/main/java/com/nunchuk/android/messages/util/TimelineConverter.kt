package com.nunchuk.android.messages.util

import com.nunchuk.android.core.util.gson
import com.nunchuk.android.messages.components.detail.*
import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

fun List<TimelineEvent>.toMessages(chatId: String) = sortedBy { it.root.ageLocalTs }.mapNotNull { it.toMessageSafe(chatId) }

fun TimelineEvent.toMessageSafe(chatId: String): Message? = try {
    toMessage(chatId)
} catch (e: Exception) {
    CrashlyticsReporter.recordException(e)
    null
}

fun TimelineEvent.toMessage(chatId: String): Message {
    return when {
        isNunchukWalletEvent() -> {
            val content = root.content?.toMap().orEmpty()
            val msgType = WalletEventType.of(content[KEY] as String)
            NunchukWalletMessage(
                sender = senderInfo,
                content = gson.toJson(root.getClearContent()),
                time = time(),
                timelineEvent = this,
                eventType = root.type!!,
                msgType = WalletEventType.of(content[KEY] as String),
                type = if (msgType == WalletEventType.INIT) MessageType.TYPE_NUNCHUK_WALLET_CARD.index else MessageType.TYPE_NUNCHUK_WALLET_NOTIFICATION.index,
                isOwner = chatId == senderInfo.userId
            )
        }
        isNunchukTransactionEvent() -> {
            val content = root.content?.toMap().orEmpty()
            val msgType = TransactionEventType.of(content[KEY] as String)
            NunchukTransactionMessage(
                sender = senderInfo,
                content = gson.toJson(root.getClearContent()),
                time = time(),
                timelineEvent = this,
                eventType = root.type!!,
                msgType = TransactionEventType.of(content[KEY] as String),
                type = if (msgType == TransactionEventType.INIT) MessageType.TYPE_NUNCHUK_TRANSACTION_CARD.index else MessageType.TYPE_NUNCHUK_TRANSACTION_NOTIFICATION.index,
                isOwner = chatId == senderInfo.userId
            )
        }
        isMessageEvent() -> {
            MatrixMessage(
                sender = senderInfo,
                content = root.getClearContent().toModel<MessageContent>()?.body.orEmpty(),
                state = root.sendState,
                time = time(),
                timelineEvent = this,
                type = chatType(chatId)
            )
        }
        else -> {
            NotificationMessage(
                sender = senderInfo,
                content = gson.toJson(root.getClearContent()),
                time = time(),
                timelineEvent = this
            )
        }
    }
}
