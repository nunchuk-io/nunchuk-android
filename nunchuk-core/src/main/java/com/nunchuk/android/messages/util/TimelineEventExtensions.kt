package com.nunchuk.android.messages.util

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.messages.components.detail.MessageType
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomNameContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent

internal const val TAG = "TimelineEvent"

fun TimelineEvent.lastMessage() = "${lastMessageSender()}: ${lastMessageContent()}"

fun TimelineEvent.lastMessageContent() = getLastMessageContentSafe() ?: getTextEditableContentSafe()

fun TimelineEvent.getLastMessageContentSafe() = try {
    if (isEncryptedEvent()) {
        STATE_ENCRYPTED_MESSAGE
    } else {
        getLastMessageContent()?.body
    }
} catch (e: Throwable) {
    null
}

fun TimelineEvent.getTextEditableContentSafe() = try {
    getTextEditableContent()
} catch (e: Throwable) {
    ""
}

fun TimelineEvent.lastMessageSender() = senderInfo.disambiguatedDisplayName

fun TimelineEvent.membership(): Membership {
    val content = root.getClearContent().toModel<RoomMemberContent>()
    return content?.membership ?: Membership.NONE
}

fun TimelineEvent.nameChange() = root.getClearContent().toModel<RoomNameContent>()?.name

fun TimelineEvent.toNunchukMatrixEvent() = NunchukMatrixEvent(
    eventId = root.eventId!!,
    type = root.getClearType(),
    content = gson.toJson(root.getClearContent()?.toMap().orEmpty()),
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

fun SenderInfo?.displayNameOrId(): String = this?.displayName ?: this?.userId ?: "Guest"

fun TimelineEvent.getBodyElementValueByKey(key: String): String {
    var element: JsonElement? = null
    return try {
        val map = root.getClearContent()?.toMap().orEmpty()
        if (map.containsKey("body")) {
            element = gson.fromJson(gson.toJson(map["body"]), JsonObject::class.java).get(key)
            element?.asString ?: ""
        } else ""
    } catch (t: Throwable) {
        CrashlyticsReporter.recordException(t)
        element?.toString()?.replace("\"", "") ?: ""
    }
}

fun TimelineEvent.isInitTransactionEvent() = isTransactionEvent(TransactionEventType.INIT)

fun TimelineEvent.isReceiveTransactionEvent() = isTransactionEvent(TransactionEventType.RECEIVE)

fun TimelineEvent.isTransactionReadyEvent() = isTransactionEvent(TransactionEventType.READY)

fun TimelineEvent.isWalletReadyEvent() = isWalletEvent(WalletEventType.READY)

private fun TimelineEvent.isTransactionEvent(type: TransactionEventType) = try {
    val content = root.getClearContent()?.toMap().orEmpty()
    val msgType = TransactionEventType.of(content[KEY] as String)
    msgType == type
} catch (t: Throwable) {
    CrashlyticsReporter.recordException(t)
    false
}

private fun TimelineEvent.isWalletEvent(type: WalletEventType) = try {
    val content = root.getClearContent()?.toMap().orEmpty()
    val msgType = WalletEventType.of(content[KEY] as String)
    msgType == type
} catch (t: Throwable) {
    CrashlyticsReporter.recordException(t)
    false
}

fun TimelineEvent.getNunchukInitEventId(): String? {
    val map = root.getClearContent()?.toMap().orEmpty()
    return gson.fromJson(gson.toJson(map["body"]), JsonObject::class.java)
        ?.getAsJsonObject("io.nunchuk.relates_to")
        ?.getAsJsonObject("init_event")
        ?.get("event_id")
        ?.asString
}
