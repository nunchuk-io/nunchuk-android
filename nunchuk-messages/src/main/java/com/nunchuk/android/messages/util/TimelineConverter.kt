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

package com.nunchuk.android.messages.util

import com.nunchuk.android.core.util.gson
import com.nunchuk.android.messages.components.detail.*
import com.nunchuk.android.messages.components.detail.MessageType
import com.nunchuk.android.messages.components.detail.model.RoomMediaSource
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.TransactionExtended
import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.api.session.crypto.attachments.toElementToDecrypt
import org.matrix.android.sdk.api.session.events.model.*
import org.matrix.android.sdk.api.session.room.model.message.*
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.MimeTypes

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
        root.isImageMessage() -> root.getClearContent().toModel<MessageImageContent>()
            ?.let { messageImageContent ->
                NunchukMediaMessage(
                    sender = senderInfo,
                    eventId = eventId,
                    time = time(),
                    filename = messageImageContent.body,
                    mimeType = messageImageContent.mimeType,
                    content = messageImageContent.getFileUrl().orEmpty(),
                    elementToDecrypt = messageImageContent.encryptedFileInfo?.toElementToDecrypt(),
                    height = messageImageContent.info?.height,
                    width = messageImageContent.info?.width,
                    allowNonMxcUrls = false,
                    isMine = chatId == senderInfo.userId,
                    error = root.sendStateError()?.message
                )
            }
        root.isVideoMessage() -> root.getClearContent().toModel<MessageVideoContent>()
            ?.let { messageVideoContent ->
                val videoInfo = messageVideoContent.videoInfo
                NunchukMediaMessage(
                    sender = senderInfo,
                    eventId = eventId,
                    time = time(),
                    filename = messageVideoContent.body,
                    mimeType = videoInfo?.mimeType,
                    content = videoInfo?.getThumbnailUrl().orEmpty(),
                    elementToDecrypt = videoInfo?.thumbnailFile?.toElementToDecrypt(),
                    height = videoInfo?.thumbnailInfo?.height,
                    width = videoInfo?.thumbnailInfo?.width,
                    allowNonMxcUrls = false,
                    isMine = chatId == senderInfo.userId,
                    error = root.sendStateError()?.message
                )
            }
        root.isFileMessage() -> root.getClearContent().toModel<MessageFileContent>()?.let { messageFileContent ->
            NunchukFileMessage(
                sender = senderInfo,
                eventId = eventId,
                time = time(),
                filename = messageFileContent.body,
                mimeType = messageFileContent.mimeType,
                content = messageFileContent.getFileUrl().orEmpty(),
                elementToDecrypt = messageFileContent.encryptedFileInfo?.toElementToDecrypt(),
                isMine = chatId == senderInfo.userId,
                error = root.sendStateError()?.message
            )
        }
        root.isTextMessage() -> {
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

fun List<TimelineEvent>.toMediaSources(): List<RoomMediaSource> = mapNotNull { it.toMediaSource() }

fun TimelineEvent.toMediaSource(): RoomMediaSource? {
    return when {
        root.isImageMessage() -> root.getClearContent().toModel<MessageImageContent>()
            ?.let { messageImageContent ->
                if (messageImageContent.mimeType == MimeTypes.Gif) {
                    RoomMediaSource.AnimatedImage(
                        eventId = eventId,
                        allowNonMxcUrls = root.sendState.isSending(),
                        content = messageImageContent,
                        error = root.sendStateError()?.message
                    )
                } else {
                    RoomMediaSource.Image(
                        eventId = eventId,
                        allowNonMxcUrls = root.sendState.isSending(),
                        content = messageImageContent,
                        error = root.sendStateError()?.message
                    )
                }
            }
        root.isVideoMessage() -> root.getClearContent().toModel<MessageVideoContent>()
            ?.let { messageVideoContent ->
                val thumbnail = object : NunchukMedia {
                    override val filename: String = messageVideoContent.body
                    override val eventId: String = this@toMediaSource.eventId
                    override val mimeType: String? =
                        messageVideoContent.videoInfo?.thumbnailInfo?.mimeType
                    override val url: String? = messageVideoContent.videoInfo?.getThumbnailUrl()
                    override val elementToDecrypt: ElementToDecrypt? =
                        messageVideoContent.videoInfo?.thumbnailFile?.toElementToDecrypt()
                    override val error: String? = null
                }
                RoomMediaSource.Video(
                    eventId,
                    allowNonMxcUrls = root.sendState.isSending(),
                    messageVideoContent,
                    thumbnail,
                    error = root.sendStateError()?.message
                )
            }
        else -> null
    }
}
