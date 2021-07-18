package com.nunchuk.android.messages.util

import android.content.Context
import android.text.format.DateUtils.*
import javax.inject.Inject

class DateFormatter @Inject constructor(private val context: Context) {

    fun formatDateAndTime(ts: Long) = if (isToday(ts)) {
        val formattedTime = getRelativeTimeSpanString(
            ts,
            System.currentTimeMillis(),
            MINUTE_IN_MILLIS,
            FORMAT_ABBREV_RELATIVE
        )
        "$formattedTime"
    } else {
        val formattedDate = getRelativeDateTimeString(
            context,
            ts,
            System.currentTimeMillis(),
            DAY_IN_MILLIS,
            FORMAT_ABBREV_RELATIVE
        )
        "$formattedDate"
    }

}
