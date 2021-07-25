package com.nunchuk.android.core.util

import org.junit.Assert
import org.junit.Test

class WordingUtilTest {

    @Test
    fun shorten() {
        Assert.assertEquals("HT", "Hung Tran".shorten())
        Assert.assertEquals("H", "hung.tran".shorten())
        Assert.assertEquals("HT", "hung tran".shorten())
        Assert.assertEquals("HA", "hung tran anh".shorten())
    }

}