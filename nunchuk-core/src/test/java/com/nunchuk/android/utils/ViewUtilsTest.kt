package com.nunchuk.android.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewUtilsTest {

    @Test
    fun formatDate() {
        assertEquals("2021.05.25 at 10:05:58 AM", (1621911958L).formatDate())
        assertEquals("2021.05.19 at 12:15:23 PM", (1621401323L).formatDate())
    }
}