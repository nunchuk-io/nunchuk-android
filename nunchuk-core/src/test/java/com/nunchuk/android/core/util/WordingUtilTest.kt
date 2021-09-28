package com.nunchuk.android.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WordingUtilTest {

    @Test
    fun shorten() {
        assertEquals("HT", "Hung Tran".shorten())
        assertEquals("H", "hung.tran".shorten())
        assertEquals("HT", "hung tran".shorten())
        assertEquals("HA", "hung tran anh".shorten())
        assertEquals("HT", "hung  tran ".shorten())
        assertEquals("", "".shorten())
    }

}