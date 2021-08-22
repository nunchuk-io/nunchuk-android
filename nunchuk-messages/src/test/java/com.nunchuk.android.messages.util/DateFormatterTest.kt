package com.nunchuk.android.messages.util

import org.junit.Assert.assertEquals
import java.util.*

class DateFormatterTest {

    @org.junit.Test
    fun formatByWeek() {
        val date = Date(1629632655438)
        assertEquals("22/08/2021", date.simpleDateFormat())
        assertEquals("Aug 22", date.formatByWeek())
        assertEquals("Sunday", date.formatByDay())
        assertEquals("06:44 PM", date.formatByHour())
    }
}