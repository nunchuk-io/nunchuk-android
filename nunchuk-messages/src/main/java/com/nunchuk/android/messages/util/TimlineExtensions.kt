package com.nunchuk.android.messages.util

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

fun TimelineEvent.isMessageEvent() = root.getClearType() == EventType.MESSAGE

fun TimelineEvent.toMessage() = root
    .getClearContent()
    .toModel<MessageContent>()?.body.orEmpty()