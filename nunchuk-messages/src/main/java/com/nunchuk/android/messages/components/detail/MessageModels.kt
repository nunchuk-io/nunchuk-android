package com.nunchuk.android.messages.components.detail

import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import java.io.Serializable

open class Message(
    open val sender: String,
    open val content: String = "",
    open val state: SendState = SendState.UNKNOWN,
    open val time: Long,
    open val type: Int
) : Serializable

data class NotificationMessage(
    override val sender: String,
    override val content: String,
    override val time: Long,
    val timelineEvent: TimelineEvent
) : Message(
    sender,
    content,
    SendState.UNKNOWN,
    time,
    MessageType.TYPE_NOTIFICATION.index
)

enum class MessageType(val index: Int) {
    TYPE_CHAT_MINE(0),
    TYPE_CHAT_PARTNER(1),
    TYPE_NOTIFICATION(2),
    TYPE_DATE(3)
}

abstract class AbsChatModel {
    abstract fun getType(): Int
}

class DateModel(val date: String) : AbsChatModel() {
    override fun getType(): Int = MessageType.TYPE_DATE.index
}

class MessageModel(val message: Message) : AbsChatModel() {
    override fun getType(): Int = message.type
}
