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

import android.net.Uri
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.RoomWalletData
import com.nunchuk.android.model.TransactionExtended

data class RoomDetailState(
    val roomInfo: RoomInfo,
    val roomWallet: RoomWallet?,
    val messages: List<Message>,
    val transactions: List<TransactionExtended>,
    val isSelectEnable: Boolean,
    val selectedEventIds: MutableSet<Long> = mutableSetOf(),
    val isSupportRoom: Boolean = false
) {

    companion object {
        fun empty() = RoomDetailState(RoomInfo.empty(), null, emptyList(), emptyList(), false)
    }

}

data class RoomInfo(val roomName: String, val memberCount: Int) {
    companion object {
        fun empty() = RoomInfo("", 0)
    }
}

sealed class RoomDetailEvent {
    object RoomNotFoundEvent : RoomDetailEvent()
    object ContactNotFoundEvent : RoomDetailEvent()
    object CreateNewSharedWallet : RoomDetailEvent()
    data class ReceiveBTCEvent(val walletId: String) : RoomDetailEvent()
    data class CreateNewTransaction(
        val roomId: String, val walletId: String, val availableAmount: Double
    ) : RoomDetailEvent()

    object OpenChatInfoEvent : RoomDetailEvent()
    object OpenChatGroupInfoEvent : RoomDetailEvent()
    object RoomWalletCreatedEvent : RoomDetailEvent()
    object HideBannerNewChatEvent : RoomDetailEvent()
    data class ViewWalletConfigEvent(
        val roomId: String, val roomWalletData: RoomWalletData
    ) : RoomDetailEvent()

    object HasUpdatedEvent : RoomDetailEvent()
    object GetRoomWalletSuccessEvent : RoomDetailEvent()
    object LeaveRoomEvent : RoomDetailEvent()
    object None : RoomDetailEvent()
    object OnSendMediaSuccess : RoomDetailEvent()
    data class ShowError(val message: String) : RoomDetailEvent()
    data class Loading(val isLoading: Boolean) : RoomDetailEvent()

    data class OpenFile(val uri: Uri, val mimeType: String?) : RoomDetailEvent()
}