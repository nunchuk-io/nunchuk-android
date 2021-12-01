package com.nunchuk.android.messages.util

import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

fun List<TimelineEvent>.groupEvents(turnOn: Boolean = true): List<TimelineEvent> {
    if (!turnOn) return this

    val allEvents = ArrayList<TimelineEvent>()
    val readyEvents = ArrayList<TimelineEvent>()
    forEach {
        if (it.isNunchukTransactionEvent() && it.isTransactionReadyEvent()) {
            if (!readyEvents.containTimelineEvent(it)) {
                allEvents.add(it)
                readyEvents.add(it)
            }
        } else if (it.isNunchukWalletEvent() && it.isWalletReadyEvent()) {
            if (!readyEvents.containTimelineEvent(it)) {
                allEvents.add(it)
                readyEvents.add(it)
            }
        } else {
            allEvents.add(it)
        }
    }
    return allEvents
}

fun List<TimelineEvent>.containTimelineEvent(event: TimelineEvent): Boolean {
    val initEventId = event.getNunchukInitEventId()
    val initEventIds = mapNotNull(TimelineEvent::getNunchukInitEventId)
    return initEventId != null && initEventId in initEventIds
}
