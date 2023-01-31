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

package com.nunchuk.android.messages.util

import com.nunchuk.android.messages.components.detail.NunchukMedia
import com.nunchuk.android.messages.components.detail.RoomInfo
import com.nunchuk.android.messages.components.list.DIRECT_CHAT_MEMBERS_COUNT
import com.nunchuk.android.messages.components.list.getRoomName
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.file.FileService
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomSummary

fun Room.isDirectChat(): Boolean {
    val roomMembers = getRoomMemberList()
    return roomSummary()?.isDirect.orFalse() || roomMembers.size == DIRECT_CHAT_MEMBERS_COUNT
}

fun Room.getRoomInfo(currentName: String): RoomInfo {
    val roomMembers = getRoomMemberList()
    val roomSummary: RoomSummary? = roomSummary()
    return if (roomSummary != null) {
        RoomInfo(roomSummary.getRoomName(currentName), roomMembers.size)
    } else {
        RoomInfo.empty()
    }
}

fun Room.getRoomMemberList() = membershipService().getRoomMembers(roomMemberQueryParams())

private fun roomMemberQueryParams() = RoomMemberQueryParams.Builder().build()

suspend fun FileService.downloadFile(data: NunchukMedia) = downloadFile(
    fileName = data.filename,
    mimeType = data.mimeType,
    url = data.url,
    elementToDecrypt = data.elementToDecrypt
)