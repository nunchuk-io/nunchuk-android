package com.nunchuk.android.messages.util

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent

fun TimelineEvent.isMessageEvent() = root.getClearType() == EventType.MESSAGE

fun TimelineEvent.lastMessage(): CharSequence {
    val senderName = senderInfo.disambiguatedDisplayName
    val lastMessage = getTextEditableContent() ?: getLastMessageContent()?.body
    return "$senderName: $lastMessage"
}