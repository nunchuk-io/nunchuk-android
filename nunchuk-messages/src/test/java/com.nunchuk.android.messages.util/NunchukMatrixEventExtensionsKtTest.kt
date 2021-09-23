package com.nunchuk.android.messages.util

import com.nunchuk.android.model.NunchukMatrixEvent
import org.junit.Assert
import org.junit.Test

class NunchukMatrixEventExtensionsKtTest {

    @Test
    fun isLocalEvent() {
        val event = NunchukMatrixEvent().copy(eventId = "\$local.342acde5-ea99-40a7-be6c-b094040d9aec")
        Assert.assertTrue(event.isLocalEvent())
    }

}