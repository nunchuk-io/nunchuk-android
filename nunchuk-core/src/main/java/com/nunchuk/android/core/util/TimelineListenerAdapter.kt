package com.nunchuk.android.core.util

import com.nunchuk.android.messages.util.isNunchukErrorEvent
import com.nunchuk.android.messages.util.isNunchukEvent
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber

const val PAGINATION = 20

class TimelineListenerAdapter : Timeline.Listener {

    private val _data = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val data = _data.asStateFlow()

    override fun onNewTimelineEvents(eventIds: List<String>) {
        Timber.d("onNewTimelineEvents($eventIds)")
    }

    override fun onTimelineFailure(throwable: Throwable) {
        CrashlyticsReporter.recordException(throwable)
    }

    override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
        _data.value = snapshot
    }

    fun getLastTimeEvents() = _data.value

    fun getNunchukEvents() = _data.value.filter(TimelineEvent::isNunchukEvent).filterNot(TimelineEvent::isNunchukErrorEvent)
}