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

package com.nunchuk.android.core.matrix

import org.matrix.android.sdk.api.provider.RoomDisplayNameFallbackProvider


class RoomDisplayNameFallbackProviderImpl : RoomDisplayNameFallbackProvider {

    override fun getNameForRoomInvite() = "Room invite"

    override fun getNameForEmptyRoom(
        isDirect: Boolean,
        leftMemberNames: List<String>
    ) = "Empty room"

    override fun getNameFor1member(name: String) = name

    override fun getNameFor2members(
        name1: String,
        name2: String
    ) = "$name1 and $name2"

    override fun getNameFor3members(
        name1: String,
        name2: String,
        name3: String
    ) = "$name1, $name2 and $name3"

    override fun getNameFor4members(
        name1: String,
        name2: String,
        name3: String,
        name4: String
    ) = "$name1, $name2, $name3 and $name4"

    override fun getNameFor4membersAndMore(
        name1: String,
        name2: String,
        name3: String,
        remainingCount: Int
    ) = "$name1, $name2, $name3 and $remainingCount others"
}
