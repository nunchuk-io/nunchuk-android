/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

import android.content.Context
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.messages.components.detail.MessageType
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.session.events.model.isFileMessage
import org.matrix.android.sdk.api.session.events.model.isImageMessage
import org.matrix.android.sdk.api.session.events.model.isVideoMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomNameContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent

internal const val TAG = "TimelineEvent"

fun TimelineEvent.lastMessage(context: Context) = "${lastMessageSender()}: ${lastMessageContent(context)}"

fun TimelineEvent.lastMessageContent(context: Context) = when {
    root.isImageMessage() -> context.getString(R.string.nc_photo_was_uploaded)
    root.isVideoMessage() -> context.getString(R.string.nc_video_was_uploaded)
    root.isFileMessage() -> context.getString(R.string.nc_attachment_was_uploaded)
    else -> getLastMessageContentSafe() ?: getTextEditableContentSafe()
}

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
    getTextEditableContent(false)
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
    return try {
        val map = root.getClearContent()?.toMap().orEmpty()
        gson.fromJson(gson.toJson(map["body"]), JsonObject::class.java)
            ?.getAsJsonObject("io.nunchuk.relates_to")
            ?.getAsJsonObject("init_event")
            ?.get("event_id")
            ?.asString
    } catch (t: Throwable) {
        CrashlyticsReporter.recordException(t)
        null
    }
}
