package com.nunchuk.android.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilTest {

    @Test
    fun formatDate() {
        assertEquals("05/25/2021 at 10:05 AM", (1621911958L).formatDate())
        assertEquals("05/19/2021 at 12:15 PM", (1621401323L).formatDate())
    }

    @Test
    fun displayedText() {
        assertEquals("12.15", (12.15).formatDecimal())
        assertEquals("0.0001", (0.00010).formatDecimal())
        assertEquals("0.00000001", (0.00000001).formatDecimal())
    }
}