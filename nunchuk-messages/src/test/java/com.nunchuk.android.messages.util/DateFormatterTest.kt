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

package com.nunchuk.android.messages.util

import com.nunchuk.android.utils.formatByDay
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.formatByWeek
import com.nunchuk.android.utils.simpleDateFormat
import org.junit.Assert.assertEquals
import java.util.Date

class DateFormatterTest {

    @org.junit.Test
    fun formatByWeek() {
        val date = Date(1629632655438)
        assertEquals("22/08/2021", date.simpleDateFormat())
        assertEquals("Aug 22", date.formatByWeek())
        assertEquals("Sunday", date.formatByDay())
        assertEquals("06:44 PM", date.formatByHour())
    }
}