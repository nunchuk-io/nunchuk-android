package com.nunchuk.android.messages.components.detail

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomWalletDataTest {
    private val gson = Gson()

    @Test
    fun parseData() {
        val body = "{\"address_type\":\"NATIVE_SEGWIT\",\"chain\":\"TESTNET\",\"description\":\"\",\"is_escrow\":false,\"m\":2.0,\"members\":[],\"n\":2.0,\"name\":\"MCU\"}"
        val data = body.toRoomWalletData(gson)
        println(data)
        assertEquals("MCU", data.name)
        assertEquals(2, data.totalSigners)
        assertEquals(2, data.requireSigners)
    }
}