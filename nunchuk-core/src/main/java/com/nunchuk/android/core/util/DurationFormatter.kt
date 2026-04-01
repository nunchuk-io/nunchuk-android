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

package com.nunchuk.android.core.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.nunchuk.android.core.R
import java.util.Locale

/**
 * Formats duration in milliseconds to hour and minute string.
 * If less than 1 hour, shows minutes only (e.g. "45m").
 * Otherwise shows hours and minutes (e.g. "1h:30m").
 */
fun Long.formatDurationHoursMinutes(): String {
    val totalMinutes = (this / (60 * 1000)).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours < 1 -> String.format(Locale.getDefault(), "%dm", minutes)
        else -> String.format(Locale.getDefault(), "%dh:%02dm", hours, minutes)
    }
}

@Composable
fun formatSecondsToPluralHoursMinutes(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours == 0 -> pluralStringResource(R.plurals.nc_plural_minute, minutes, minutes)
        minutes == 0 -> pluralStringResource(R.plurals.nc_plural_hour, hours, hours)
        else -> "${pluralStringResource(R.plurals.nc_plural_hour, hours, hours)} ${
            pluralStringResource(R.plurals.nc_plural_minute, minutes, minutes)
        }"
    }
}
