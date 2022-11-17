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
