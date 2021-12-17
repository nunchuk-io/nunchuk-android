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
