package com.nunchuk.android.messages.util

import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings

fun Room.addMessageListener(callback: (List<TimelineEvent>) -> Unit = {}) {

    createTimeline(null, TimelineSettings(initialSize = 100)).apply {
        addListener(TimelineListenerAdapter {
            val messages = it.filter(TimelineEvent::isMessageEvent)
            callback(messages)
        })
        start()
    }

}