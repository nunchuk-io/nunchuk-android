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