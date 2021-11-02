package com.nunchuk.android.core.util

import org.junit.Assert
import org.junit.Test

class WalletUtilTest {

    @Test
    fun toAmount() {
        val btc = 0.0003
        val amount = btc.toAmount()
        Assert.assertEquals(30000, amount.value)
        Assert.assertEquals("$13.50 USD", amount.getUSDAmount())
    }

}