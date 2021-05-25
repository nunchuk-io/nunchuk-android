package com.nunchuk.android.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewUtilsTest {

    @Test
    fun formatDate() {
        assertEquals("25/05/2021 at 10:05 AM", (1621911958L).formatDate())
        assertEquals("19/05/2021 at 12:15 PM", (1621401323L).formatDate())
    }
}