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

package com.nunchuk.android.utils

import java.text.SimpleDateFormat
import java.util.*

private val SIMPLE_DATE = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
private val SIMPLE_GLOBAL_DATE = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
private val SIMPLE_WEEK_DAY_YEAR = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
private val SIMPLE_HOUR_MINUTE = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
private val WEEK_DAY_YEAR = SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH)
private val DATE_TIME_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH)

fun Date.formatMessageDate(showToday: Boolean = false): String {
    val messageDate: Calendar = Calendar.getInstance()
    val currentDate: Calendar = Calendar.getInstance()
    messageDate.time = this
    currentDate.time = Date()
    return when {
        !sameYear(messageDate, currentDate) -> formatByYear()
        !sameWeek(messageDate, currentDate) -> formatByWeek()
        !sameDay(messageDate, currentDate) -> formatByDay()
        else -> if (showToday) "Today" else formatByHour()
    }
}

private fun sameYear(messageDate: Calendar, currentDate: Calendar): Boolean {
    return messageDate.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR)
}

private fun sameWeek(messageDate: Calendar, currentDate: Calendar): Boolean {
    return messageDate.get(Calendar.WEEK_OF_YEAR) == currentDate.get(Calendar.WEEK_OF_YEAR)
}

private fun sameDay(messageDate: Calendar, currentDate: Calendar): Boolean {
    return messageDate.get(Calendar.DAY_OF_YEAR) == currentDate.get(Calendar.DAY_OF_YEAR)
}

internal fun Date.formatByYear() = simpleDateFormat()

fun Date.formatByWeek(): String = SimpleDateFormat("MMM dd", Locale.ENGLISH).format(this)

fun Date.formatByDay(): String = SimpleDateFormat("EEEE", Locale.ENGLISH).format(this)

fun Date.formatByHour(): String = SIMPLE_HOUR_MINUTE.format(this)

fun Date.simpleDateFormat(): String = SIMPLE_DATE.format(this)

fun Date.simpleWeekDayYearFormat(): String = SIMPLE_WEEK_DAY_YEAR.format(this)
fun Date.weekDayYearFormat(): String = WEEK_DAY_YEAR.format(this)

fun Date.simpleGlobalDateFormat(): String = SIMPLE_GLOBAL_DATE.format(this)

fun Date.dateTimeFormat(): String = DATE_TIME_FORMAT.format(this)

fun String.simpleDateFormat(): Date = SIMPLE_DATE.parse(this) ?: Date()
