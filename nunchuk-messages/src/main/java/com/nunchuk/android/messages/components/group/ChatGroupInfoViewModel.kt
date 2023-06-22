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

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.SendErrorEventUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.messages.components.group.ChatGroupInfoEvent.*
import com.nunchuk.android.messages.util.getRoomMemberList
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.usecase.GetRoomWalletUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.room.Room
import javax.inject.Inject

// TODO eliminate duplicated
@HiltViewModel
class ChatGroupInfoViewModel @Inject constructor(
    private val getRoomWalletUseCase: GetRoomWalletUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val sendErrorEventUseCase: SendErrorEventUseCase,
    private val sessionHolder: SessionHolder
) : NunchukViewModel<ChatGroupInfoState, ChatGroupInfoEvent>() {

    private lateinit var room: Room

    override val initialState = ChatGroupInfoState()

    fun initialize(roomId: String) {
        sessionHolder.getSafeActiveSession()?.roomService()?.getRoom(roomId)?.let(::onRetrievedRoom) ?: event(RoomNotFoundEvent)
    }

    private fun onRetrievedRoom(room: Room) {
        this.room = room
        room.roomSummary()?.let {
            updateState { copy(summary = it) }
        }
        updateState { copy(roomMembers = room.getRoomMemberList()) }
        getRoomWallet()
    }

    private fun getRoomWallet() {
        viewModelScope.launch {
            getRoomWalletUseCase.execute(roomId = room.roomId)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect { onGetRoomWallet(it) }
        }

    }

    private fun onGetRoomWallet(roomWallet: RoomWallet?) {
        updateState { copy(roomWallet = roomWallet) }
        viewModelScope.launch {
            roomWallet?.let { rWallet ->
                getWalletUseCase.execute(walletId = rWallet.walletId)
                    .onException { }
                    .flowOn(Dispatchers.Main)
                    .collect { onGetWallet(it.wallet) }
            }
        }
    }

    private fun onGetWallet(wallet: Wallet) {
        updateState { copy(wallet = wallet) }
    }

    fun handleEditName(name: String) {
        viewModelScope.launch {
            try {
                room.stateService().updateName(name)
                event(UpdateRoomNameSuccess(name))
            } catch (e: Throwable) {
                CrashlyticsReporter.recordException(e)
                event(UpdateRoomNameError(e.toMatrixError()))
                sendErrorEvent(roomId = room.roomId, e, sendErrorEventUseCase::execute)
            }
        }
    }

    fun handleLeaveGroup() {
        viewModelScope.launch {
            try {
                sessionHolder.getSafeActiveSession()?.roomService()?.leaveRoom(room.roomId)
                event(LeaveRoomSuccess)
            } catch (e: Throwable) {
                CrashlyticsReporter.recordException(e)
                event(LeaveRoomError(e.toMatrixError()))
                sendErrorEvent(roomId = room.roomId, e, sendErrorEventUseCase::execute)
            }
        }
    }

    fun createWalletOrTransaction() {
        val wallet = getState().wallet
        if (wallet == null) {
            event(CreateSharedWalletEvent)
        } else if (wallet.balance.value > 0L) {
            event(CreateTransactionEvent(room.roomId, wallet.id, wallet.balance.pureBTC()))
        }
    }
}

fun Throwable.toMatrixError() = if (this is Failure.ServerError) {
    error.message
} else {
    message.orUnknownError()
}