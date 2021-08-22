package com.nunchuk.android.messages.components.detail

import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.send.SendState
import java.io.Serializable

open class Message(
    open val sender: String,
    open val content: String = "",
    open val state: SendState = SendState.UNKNOWN,
    open val type: Int
) : Serializable

data class NotificationMessage(
    override val sender: String,
    override val type: Int,
    override val content: String,
    val membership: Membership
) : Message(sender, content, SendState.UNKNOWN, type)

enum class MessageType(val index: Int) {
    CHAT_MINE(0),
    CHAT_PARTNER(1),
    NOTIFICATION(2)
}
