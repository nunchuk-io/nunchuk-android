package com.nunchuk.android.messages.util

import com.google.gson.Gson
import com.nunchuk.android.messages.components.detail.Message
import com.nunchuk.android.messages.components.detail.MessageType
import org.json.JSONArray
import org.json.JSONObject
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent

fun String.toContent() = JSONObject(this).toMap().toContent()

fun JSONObject.toMap(): Map<String, *> = keys().asSequence().associateWith {
    when (val value = this[it]) {
        is JSONArray -> {
            val map = (0 until value.length()).associate { "$it" to value[it] }
            JSONObject(map).toMap().values.toList()
        }
        is JSONObject -> value.toMap()
        JSONObject.NULL -> null
        else -> value
    }
}

fun TimelineEvent.isDisplayable() = isMessageEvent() || isNunchukEvent()

fun TimelineEvent.isMessageEvent() = root.getClearType() == EventType.MESSAGE

fun TimelineEvent.isNunchukEvent() = root.type == "io.nunchuk.wallet"

fun TimelineEvent.lastMessage(): CharSequence {
    val senderName = senderInfo.disambiguatedDisplayName
    val lastMessage = getTextEditableContent() ?: getLastMessageContent()?.body
    return "$senderName: $lastMessage"
}

fun TimelineEvent.toMessage(chatId: String): Message {
    return if (isNunchukEvent()) {
        Message(
            sender = senderInfo.displayName ?: "Guest",
            content = Gson().toJson(root.getClearContent()),
            type = if (chatId == senderInfo.userId) MessageType.CHAT_MINE.index else MessageType.CHAT_PARTNER.index
        )
    } else {
        Message(
            sender = senderInfo.displayName ?: "Guest",
            content = root.getClearContent().toModel<MessageContent>()?.body.orEmpty(),
            type = if (chatId == senderInfo.userId) MessageType.CHAT_MINE.index else MessageType.CHAT_PARTNER.index
        )
    }
}