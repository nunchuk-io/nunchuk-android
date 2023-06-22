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

package com.nunchuk.android.messages.components.list

import com.nunchuk.android.core.util.SUPPORT_ROOM_USER_ID
import com.nunchuk.android.messages.util.STATE_NUNCHUK_SYNC
import org.matrix.android.sdk.api.session.room.model.RoomSummary

fun RoomSummary.getRoomName(currentName: String): String {
    val split = displayName.split(",")
    return if (split.size == DIRECT_CHAT_MEMBERS_COUNT) {
        split.firstOrNull { it != currentName } ?: currentName
    } else {
        displayName
    }
}

fun RoomSummary.getMembersCount() = otherMemberIds.size + 1

fun RoomSummary.isDirectChat() = isDirect || getMembersCount() <= DIRECT_CHAT_MEMBERS_COUNT

fun RoomSummary.shouldShow() = (!isServerNotices() && !isSyncRoom())

fun RoomSummary.isServerNotices() = otherMemberIds.contains(NOTICE_ROOM_INVITE_ID) || name == SERVER_NOTICES

fun RoomSummary.isSyncRoom() = tags.isNotEmpty() && tags.any { it.name == STATE_NUNCHUK_SYNC }

fun RoomSummary.isSupportRoom() = otherMemberIds.contains(SUPPORT_ROOM_USER_ID)

const val NOTICE_ROOM_INVITE_ID = "@nunchuk-notices:nunchuk.io"
const val SERVER_NOTICES = "Server Notices"

const val DIRECT_CHAT_MEMBERS_COUNT = 2
