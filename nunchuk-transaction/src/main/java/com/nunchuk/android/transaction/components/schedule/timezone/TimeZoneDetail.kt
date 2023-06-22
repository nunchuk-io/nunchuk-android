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

package com.nunchuk.android.transaction.components.schedule.timezone

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.OffsetDateTime
import java.time.ZoneId

@Parcelize
data class TimeZoneDetail(
    val id: String = "",
    val city: String = "",
    val country: String = "",
    val offset: String = "",
) : Parcelable

fun String.toTimeZoneDetail(): TimeZoneDetail? {
    val zone = ZoneId.of(this)
    val offsetToday = OffsetDateTime.now(zone).offset
    if (offsetToday.id == "Z") return null

    val tokens = this.replace("_", " ").split("/")
    if (tokens.size != 2) return null
    return TimeZoneDetail(
        id = this,
        country = tokens[0],
        city = tokens[1],
        offset = "GMT${offsetToday.id}"
    )
}