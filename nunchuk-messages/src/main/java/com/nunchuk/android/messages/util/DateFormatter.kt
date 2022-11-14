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

import java.text.SimpleDateFormat
import java.util.*

val SIMPLE_DATE = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

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

fun Date.formatByHour(): String = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(this)

fun Date.simpleDateFormat(): String = SIMPLE_DATE.format(this)

internal fun String.simpleDateFormat(): Date = SIMPLE_DATE.parse(this) ?: Date()
