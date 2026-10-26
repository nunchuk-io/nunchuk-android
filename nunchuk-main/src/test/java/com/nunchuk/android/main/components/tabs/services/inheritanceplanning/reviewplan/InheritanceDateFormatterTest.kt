package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class InheritanceDateFormatterTest {

    private lateinit var deviceZone: TimeZone

    // 2035-05-28 18:25 UTC
    private val timestamp = 2_063_989_500_000L

    @Before
    fun setUp() {
        deviceZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(deviceZone)
    }

    @Test
    fun `formats in the labelled zone, not the device zone`() {
        assertEquals(
            "05/29/2035 01:25",
            formatDateTimeInTimezone(timestamp, isOnChainTimelock = true, timeZoneId = "Asia/Jakarta"),
        )
    }

    @Test
    fun `blank or unknown zone falls back to the device zone`() {
        assertEquals("05/28/2035 18:25", formatDateTimeInTimezone(timestamp, isOnChainTimelock = true))
        assertEquals(
            "05/28/2035 18:25",
            formatDateTimeInTimezone(timestamp, isOnChainTimelock = true, timeZoneId = "Not/AZone"),
        )
    }
}
