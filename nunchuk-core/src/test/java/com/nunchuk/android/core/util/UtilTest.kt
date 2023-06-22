/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilTest {

    @Test
    fun formatDate() {
        assertEquals("05/25/2021 at 10:05 AM", (1621911958L).formatDate())
        assertEquals("05/19/2021 at 12:15 PM", (1621401323L).formatDate())
    }

    @Test
    fun displayedText() {
        assertEquals("12.15", (12.15).formatDecimal())
        assertEquals("0.0001", (0.00010).formatDecimal())
        assertEquals("0.00000001", (0.00000001).formatDecimal())
    }
}