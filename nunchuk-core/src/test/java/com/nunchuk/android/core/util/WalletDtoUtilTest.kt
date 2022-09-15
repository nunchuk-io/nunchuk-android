package com.nunchuk.android.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WalletDtoUtilTest {

    @Test
    fun toAmount() {
        val btc = 0.0003
        val amount = btc.toAmount()
        assertEquals(30000, amount.value)
        assertEquals("$13.50", amount.getUSDAmount())
    }

}