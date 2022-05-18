package com.nunchuk.android.core.util

import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber

const val PAGINATION = 20

open class TimelineListenerAdapter(val callback: (List<TimelineEvent>) -> Unit = {}) : Timeline.Listener {

    private var lastSnapshot: List<TimelineEvent> = ArrayList()

    override fun onNewTimelineEvents(eventIds: List<String>) {
        Timber.d("onNewTimelineEvents($eventIds)")
    }

    override fun onTimelineFailure(throwable: Throwable) {
        CrashlyticsReporter.recordException(throwable)
    }

    override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
        if (lastSnapshot != snapshot) {
            lastSnapshot = snapshot
            callback(snapshot)
        }
    }

}