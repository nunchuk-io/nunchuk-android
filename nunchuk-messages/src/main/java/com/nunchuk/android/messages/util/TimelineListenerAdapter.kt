package com.nunchuk.android.messages.util

import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

open class TimelineListenerAdapter(val callback: (List<TimelineEvent>) -> Unit = {}) : Timeline.Listener {

    private var lastSnapshot: List<TimelineEvent> = ArrayList()

    override fun onNewTimelineEvents(eventIds: List<String>) {
    }

    override fun onTimelineFailure(throwable: Throwable) {
    }

    override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
        if (lastSnapshot != snapshot) {
            lastSnapshot = snapshot
            callback(snapshot)
        }
    }

}