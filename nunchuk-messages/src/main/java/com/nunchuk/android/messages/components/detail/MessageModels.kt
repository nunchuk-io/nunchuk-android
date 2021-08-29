package com.nunchuk.android.messages.components.detail

import com.nunchuk.android.messages.util.WalletEventType
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

data class NunchukWalletMessage(
    override val sender: String,
    override val content: String,
    override val time: Long,
    val timelineEvent: TimelineEvent,
    val eventType: String,
    val msgType: WalletEventType,
    override val type: Int = MessageType.TYPE_NUNCHUK_CARD.index
) : Message(
    sender,
    content,
    SendState.UNKNOWN,
    time,
    type
)