package com.nunchuk.android.messages.util

import com.nunchuk.android.core.util.gson
import com.nunchuk.android.messages.components.detail.*
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.TransactionExtended
import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

fun List<TimelineEvent>.toMessages(
    chatId: String,
    roomWallet: RoomWallet?,
    transactions: List<TransactionExtended>,
    isSelectEnable: Boolean,
    selectedEventIds: Set<Long>
) =
    sortedBy { it.root.ageLocalTs }.mapNotNull {
        it.toMessageSafe(
            chatId,
            roomWallet,
            transactions.filterNot { tx -> tx.initEventId.startsWith("\$local.") },
            isSelectEnable,
            selectedEventIds
        )
    }

fun TimelineEvent.toMessageSafe(
    chatId: String,
    roomWallet: RoomWallet?,
    transactions: List<TransactionExtended>,
    isSelectEnable: Boolean,
    selectedEventIds: Set<Long>
): Message? = try {
    toMessage(chatId, roomWallet, transactions, isSelectEnable, selectedEventIds)
} catch (e: Exception) {
    CrashlyticsReporter.recordException(e)
    null
}

fun TimelineEvent.toMessage(
    chatId: String,
    roomWallet: RoomWallet?,
    transactions: List<TransactionExtended>,
    isSelectEnable: Boolean,
    selectedEventIds: Set<Long>
): Message? {
    return when {
        isNunchukWalletEvent() -> {
            roomWallet ?: return null
            val content = root.getClearContent()?.toMap().orEmpty()
            val msgType = WalletEventType.of(content[KEY] as String)
            NunchukWalletMessage(
                sender = senderInfo,
                content = gson.toJson(root.getClearContent()),
                time = time(),
                timelineEvent = this,
                eventType = root.getClearType(),
                msgType = WalletEventType.of(content[KEY] as String),
                type = if (msgType == WalletEventType.INIT) MessageType.TYPE_NUNCHUK_WALLET_CARD.index else MessageType.TYPE_NUNCHUK_WALLET_NOTIFICATION.index,
                isOwner = chatId == senderInfo.userId,
                roomWallet = roomWallet
            )
        }
        isNunchukTransactionEvent() -> {
            val content = root.getClearContent()?.toMap().orEmpty()
            val msgType = TransactionEventType.of(content[KEY] as String)
            var walletId = getBodyElementValueByKey("wallet_id")
            if (walletId.isEmpty()) {
                walletId = transactions.firstOrNull { it.walletId.isNotEmpty() }?.walletId.orEmpty()
            }
            val transaction = transactions.find { it.initEventId == eventId } ?: return null
            NunchukTransactionMessage(
                sender = senderInfo,
                content = gson.toJson(root.getClearContent()),
                time = time(),
                timelineEvent = this,
                eventType = root.getClearType(),
                msgType = TransactionEventType.of(content[KEY] as String),
                type = if (msgType == TransactionEventType.INIT) MessageType.TYPE_NUNCHUK_TRANSACTION_CARD.index else MessageType.TYPE_NUNCHUK_TRANSACTION_NOTIFICATION.index,
                isOwner = chatId == senderInfo.userId,
                roomWallet = roomWallet,
                walletId = walletId,
                transaction = transaction.transaction
            )
        }
        isMessageEvent() -> {
            MatrixMessage(
                sender = senderInfo,
                content = root.getClearContent().toModel<MessageContent>()?.body.orEmpty(),
                state = root.sendState,
                time = time(),
                timelineEvent = this,
                type = chatType(chatId),
                isSelectEnable = isSelectEnable,
                selected = selectedEventIds.contains(localId)
            )
        }
        isEncryptedEvent() -> {
            MatrixMessage(
                sender = senderInfo,
                content = STATE_ENCRYPTED_MESSAGE,
                state = root.sendState,
                time = time(),
                timelineEvent = this,
                type = chatType(chatId),
                isSelectEnable = isSelectEnable,
                selected = selectedEventIds.contains(localId)
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
