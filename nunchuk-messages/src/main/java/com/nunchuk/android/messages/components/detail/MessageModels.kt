/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.messages.components.detail

import com.nunchuk.android.messages.util.TransactionEventType
import com.nunchuk.android.messages.util.WalletEventType
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.Transaction
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
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
    val selected: Boolean,
    val isSelectEnable: Boolean
) : Message(
    sender, content, state, time, type
)

data class NotificationMessage(
    override val sender: SenderInfo,
    override val content: String,
    override val time: Long,
    val timelineEvent: TimelineEvent
) : Message(
    sender, content, SendState.UNKNOWN, time, MessageType.TYPE_NOTIFICATION.index
)

data class NunchukWalletMessage(
    override val sender: SenderInfo,
    override val content: String,
    override val time: Long,
    val timelineEvent: TimelineEvent,
    val eventType: String,
    val msgType: WalletEventType,
    override val type: Int = MessageType.TYPE_NUNCHUK_WALLET_CARD.index,
    val isOwner: Boolean = false,
    val roomWallet: RoomWallet?
) : Message(
    sender, content, SendState.UNKNOWN, time, type
)

data class NunchukTransactionMessage(
    override val sender: SenderInfo,
    override val content: String,
    override val time: Long,
    val timelineEvent: TimelineEvent,
    val eventType: String,
    val msgType: TransactionEventType,
    override val type: Int = MessageType.TYPE_NUNCHUK_TRANSACTION_CARD.index,
    val isOwner: Boolean = false,
    val roomWallet: RoomWallet?,
    val walletId: String,
    val transaction: Transaction?
) : Message(
    sender, content, SendState.UNKNOWN, time, type
)

data class NunchukMediaMessage(
    override val sender: SenderInfo,
    override val content: String,
    override val time: Long,
    val eventId: String,
    val isMine: Boolean,
    val filename: String,
    val mimeType: String?,
    val elementToDecrypt: ElementToDecrypt?,
    val height: Int?,
    val width: Int?,
    val allowNonMxcUrls: Boolean = false
) : Message(
    sender, content, SendState.UNKNOWN, time, MessageType.TYPE_IMAGE_AND_VIDEO.index
)