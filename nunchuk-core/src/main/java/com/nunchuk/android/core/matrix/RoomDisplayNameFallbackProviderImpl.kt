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
import android.content.Context
import com.nunchuk.android.core.R


class RoomDisplayNameFallbackProviderImpl(
    private val context: Context
) : RoomDisplayNameFallbackProvider {

    override fun excludedUserIds(roomId: String): List<String> = emptyList()

    override fun getNameForRoomInvite() = context.getString(R.string.nc_matrix_room_invite)

    override fun getNameForEmptyRoom(
        isDirect: Boolean,
        leftMemberNames: List<String>
    ) = context.getString(R.string.nc_matrix_empty_room)

    override fun getNameFor1member(name: String) = name

    override fun getNameFor2members(
        name1: String,
        name2: String
    ) = context.getString(R.string.nc_matrix_room_name_two_members, name1, name2)

    override fun getNameFor3members(
        name1: String,
        name2: String,
        name3: String
    ) = context.getString(R.string.nc_matrix_room_name_three_members, name1, name2, name3)

    override fun getNameFor4members(
        name1: String,
        name2: String,
        name3: String,
        name4: String
    ) = context.getString(R.string.nc_matrix_room_name_four_members, name1, name2, name3, name4)

    override fun getNameFor4membersAndMore(
        name1: String,
        name2: String,
        name3: String,
        remainingCount: Int
    ) = context.resources.getQuantityString(R.plurals.nc_matrix_room_name_more_members, remainingCount, name1, name2, name3, remainingCount)
}
