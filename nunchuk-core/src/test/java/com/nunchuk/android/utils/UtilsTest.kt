package com.nunchuk.android.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {

    @Test
    fun formatDate() {
        assertEquals("25/05/2021 at 10:05 AM", (1621911958L).formatDate())
        assertEquals("19/05/2021 at 12:15 PM", (1621401323L).formatDate())
    }

    @Test
    fun displayedText() {
        assertEquals("12.15", (12.15).formatDecimal())
        assertEquals("0.0001", (0.00010).formatDecimal())
        assertEquals("0.00000001", (0.00000001).formatDecimal())
    }
}