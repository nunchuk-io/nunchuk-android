/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.messages.components.detail

import com.google.gson.Gson
import com.nunchuk.android.model.toRoomWalletData
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomWalletDataTest {
    private val gson = Gson()

    @Test
    fun parseData() {
        val body = "{\"address_type\":\"NATIVE_SEGWIT\",\"chain\":\"TESTNET\",\"description\":\"\",\"is_escrow\":false,\"m\":2.0,\"members\":[],\"n\":2.0,\"name\":\"MCU\"}"
        val data = body.toRoomWalletData(gson)
        assertEquals("MCU", data.name)
        assertEquals(2, data.totalSigners)
        assertEquals(2, data.requireSigners)
    }
}