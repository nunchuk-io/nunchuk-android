package com.nunchuk.android.messages.components.detail

import com.nunchuk.android.messages.util.TransactionEventType
import com.nunchuk.android.messages.util.WalletEventType
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import java.io.Serializable

open class Message(
    open val sender: SenderInfo,
    open val content: String = "",
    open val state: SendState = SendState.UNKNOWN,
    open val time: Long,
    open val type: Int
) : Serializable

data class MatrixMessage(
    override val sender: SenderInfo,
    override val content: String,
    override val state: SendState,
    override val time: Long,
    override val type: Int,
    val timelineEvent: TimelineEvent,
    val selected: Boolean = false
) : Message(
    sender,
    content,
    state,
    time,
    type
)

data class NotificationMessage(
    override val sender: SenderInfo,
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
    override val sender: SenderInfo,
    override val content: String,
    override val time: Long,
    val timelineEvent: TimelineEvent,
    val eventType: String,
    val msgType: WalletEventType,
    override val type: Int = MessageType.TYPE_NUNCHUK_WALLET_CARD.index,
    val isOwner: Boolean = false
) : Message(
    sender,
    content,
    SendState.UNKNOWN,
    time,
    type
)

data class NunchukTransactionMessage(
    override val sender: SenderInfo,
    override val content: String,
    override val time: Long,
    val timelineEvent: TimelineEvent,
    val eventType: String,
    val msgType: TransactionEventType,
    override val type: Int = MessageType.TYPE_NUNCHUK_TRANSACTION_CARD.index,
    val isOwner: Boolean = false
) : Message(
    sender,
    content,
    SendState.UNKNOWN,
    time,
    type
)