package com.nunchuk.android.messages.util

import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

fun List<TimelineEvent>.groupEvents(turnOn: Boolean = true, loadMore: () -> Unit = {}): List<TimelineEvent> {
    if (!turnOn) return this

    val allEvents = ArrayList<TimelineEvent>()
    val readyEvents = ArrayList<TimelineEvent>()
    var hasDuplicated = false
    forEach {
        if (it.isNunchukTransactionEvent() && it.isTransactionReadyEvent()) {
            if (!readyEvents.containTimelineEvent(it)) {
                allEvents.add(it)
                readyEvents.add(it)
            } else {
                hasDuplicated = true
            }
        } else if (it.isNunchukWalletEvent() && it.isWalletReadyEvent()) {
            if (!readyEvents.containTimelineEvent(it)) {
                allEvents.add(it)
                readyEvents.add(it)
            } else {
                hasDuplicated = true
            }
        } else {
            allEvents.add(it)
        }
    }
    if (hasDuplicated) {
        loadMore()
    }
    return allEvents
}

fun List<TimelineEvent>.containTimelineEvent(event: TimelineEvent): Boolean {
    val initEventId = event.getNunchukInitEventId()
    val initEventIds = mapNotNull(TimelineEvent::getNunchukInitEventId)
    return initEventId != null && initEventId in initEventIds
}
