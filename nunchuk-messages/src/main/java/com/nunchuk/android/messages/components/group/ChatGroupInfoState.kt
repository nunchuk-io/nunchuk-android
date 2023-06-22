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

package com.nunchuk.android.messages.components.group

import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.Wallet
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class ChatGroupInfoState(
    val summary: RoomSummary? = null,
    val roomMembers: List<RoomMemberSummary> = emptyList(),
    val roomWallet: RoomWallet? = null,
    val wallet: Wallet? = null
)

sealed class ChatGroupInfoEvent {
    object RoomNotFoundEvent : ChatGroupInfoEvent()
    object CreateSharedWalletEvent : ChatGroupInfoEvent()
    object LeaveRoomSuccess : ChatGroupInfoEvent()
    data class UpdateRoomNameError(val message: String) : ChatGroupInfoEvent()
    data class UpdateRoomNameSuccess(val name: String) : ChatGroupInfoEvent()
    data class LeaveRoomError(val message: String) : ChatGroupInfoEvent()
    data class CreateTransactionEvent(
        val roomId: String,
        val walletId: String,
        val availableAmount: Double
    ) : ChatGroupInfoEvent()
}