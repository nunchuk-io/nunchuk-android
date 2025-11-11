/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.core.ui.toTimeZoneDetail
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Formats a timestamp in the specified timezone
 * @param timestamp The timestamp to format
 * @param timeZoneId The timezone ID (e.g., "Africa/Bamako")
 * @param isOnChainTimelock If true, includes time (MM/dd/yyyy HH:mm), if false, date only (MM/dd/yyyy)
 * @return Formatted date string
 */
fun formatDateTimeInTimezone(timestamp: Long, timeZoneId: String, isOnChainTimelock: Boolean): String {
    if (timestamp <= 0) return ""

    val timeZone = if (timeZoneId.isNotEmpty()) {
        TimeZone.getTimeZone(timeZoneId)
    } else {
        TimeZone.getDefault()
    }

    val calendar = Calendar.getInstance(timeZone).apply {
        timeInMillis = timestamp
    }

    val formatPattern = if (isOnChainTimelock) {
        "MM/dd/yyyy HH:mm" // On-chain: include time
    } else {
        "MM/dd/yyyy" // Off-chain: date only
    }

    val dateTimeFormat = SimpleDateFormat(formatPattern, Locale.ENGLISH).apply {
        this.timeZone = timeZone
    }

    return dateTimeFormat.format(calendar.time)
}

/**
 * Formats a timestamp with date only in the specified timezone
 * Format: "MM/dd/yyyy" (e.g., "07/25/2025")
 */
fun formatDateInTimezone(timestamp: Long, timeZoneId: String): String {
    if (timestamp <= 0) return ""

    val timeZone = if (timeZoneId.isNotEmpty()) {
        TimeZone.getTimeZone(timeZoneId)
    } else {
        TimeZone.getDefault()
    }

    val calendar = Calendar.getInstance(timeZone).apply {
        timeInMillis = timestamp
    }

    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).apply {
        this.timeZone = timeZone
    }

    return dateFormat.format(calendar.time)
}

/**
 * Gets the timezone display string (e.g., "Africa/Bamako (GMT+00:00)")
 */
fun getTimezoneDisplay(timeZoneId: String): String {
    val timeZoneDetail = if (timeZoneId.isNotEmpty()) {
        timeZoneId.toTimeZoneDetail()
    } else {
        TimeZone.getDefault().id.toTimeZoneDetail()
    } ?: TimeZoneDetail()

    return if (timeZoneDetail.id.isNotEmpty() && timeZoneDetail.offset.isNotEmpty()) {
        "${timeZoneDetail.id} (${timeZoneDetail.offset})"
    } else {
        timeZoneDetail.id.ifEmpty { TimeZone.getDefault().id }
    }
}

