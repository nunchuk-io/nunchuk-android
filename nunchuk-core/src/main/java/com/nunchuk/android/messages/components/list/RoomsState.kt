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

import com.nunchuk.android.messages.usecase.message.GroupWalletMessage
import com.nunchuk.android.model.GroupChatRoom
import com.nunchuk.android.model.RoomWallet
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class RoomsState(
    val rooms: List<RoomMessage>,
    val roomWallets: List<RoomWallet>,
    val groupChatRooms: MutableMap<String, GroupChatRoom>,
    val walletIds: List<String> = emptyList(),
    val groupWalletMessages: List<GroupWalletMessage> = emptyList(),
    val matrixRooms: List<RoomSummary> = emptyList(),
) {

    companion object {
        fun empty() = RoomsState(emptyList(), emptyList(), HashMap())
    }

}

sealed class RoomMessage(val time: Long) {
    data class MatrixRoom(val data: RoomSummary) :
        RoomMessage(data.latestPreviewableEvent?.root?.originServerTs ?: 0L)

    data class GroupWalletRoom(val data: GroupWalletMessage) : RoomMessage(data.timestamp)
}

sealed class RoomsEvent {
    data class LoadingEvent(val loading: Boolean) : RoomsEvent()
    data class CreateSupportRoomSuccess(val roomId: String) : RoomsEvent()
    data class ShowError(val message: String) : RoomsEvent()
    data class RemoveRoomSuccess(val roomSummary: RoomSummary) : RoomsEvent()
}
