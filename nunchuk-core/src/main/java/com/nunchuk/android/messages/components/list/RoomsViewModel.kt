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

package com.nunchuk.android.messages.components.list

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.matrix.roomSummariesFlow
import com.nunchuk.android.core.util.SUPPORT_ROOM_TYPE
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.log.fileLog
import com.nunchuk.android.messages.usecase.message.GetOrCreateSupportRoomUseCase
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.messages.util.sortByLastMessage
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.usecase.GetAllRoomWalletsUseCase
import com.nunchuk.android.usecase.membership.GetLocalCurrentSubscriptionPlan
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

@HiltViewModel
class RoomsViewModel @Inject constructor(
    private val getAllRoomWalletsUseCase: GetAllRoomWalletsUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase,
    private val sessionHolder: SessionHolder,
    private val getOrCreateSupportRoomUseCase: GetOrCreateSupportRoomUseCase,
    getLocalCurrentSubscriptionPlan: GetLocalCurrentSubscriptionPlan,
) : NunchukViewModel<RoomsState, RoomsEvent>() {

    override val initialState = RoomsState.empty()

    val plan = getLocalCurrentSubscriptionPlan(Unit)
        .map { it.getOrElse { MembershipPlan.NONE } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MembershipPlan.NONE)

    private var listenJob: Job? = null

    init {
        listenRoomSummaries()
    }

    fun handleMatrixSignedIn() {
        listenRoomSummaries()
    }

    fun listenRoomSummaries() {
        listenJob?.cancel()
        val session = sessionHolder.getSafeActiveSession() ?: return
        listenJob = session.roomSummariesFlow()
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
            .onStart { setEvent(RoomsEvent.LoadingEvent(true)) }
            .onEach {
                fileLog("listenRoomSummaries($it)")
                leaveDraftSyncRoom(it)
                retrieveMessages(it)
            }
            .onCompletion { setEvent(RoomsEvent.LoadingEvent(false)) }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
    }

    private suspend fun retrieveMessages(rooms: List<RoomSummary>) {
        getAllRoomWalletsUseCase.execute()
            .map { wallets -> rooms to wallets }
            .flowOn(Dispatchers.IO)
            .onException { onRetrieveMessageError(it) }
            .flowOn(Dispatchers.Main)
            .onEach {
                fileLog("onRetrieveMessageSuccess")
                onRetrieveMessageSuccess(it)
            }
            .distinctUntilChanged()
            .collect()
    }

    private fun leaveDraftSyncRoom(summaries: List<RoomSummary>) {
        // delete to avoid misunderstandings
        viewModelScope.launch(Dispatchers.IO) {
            val draftSyncRooms = summaries.filter {
                it.displayName == TAG_SYNC && it.tags.isEmpty()
                        || (it.roomType == SUPPORT_ROOM_TYPE && it.joinedMembersCount == 1 && it.latestPreviewableEvent != null)
            }
            draftSyncRooms.forEach {
                leaveRoomUseCase.execute(it.roomId)
                    .flowOn(Dispatchers.IO)
                    .onException { }
                    .collect()
            }
        }
    }

    private fun onRetrieveMessageError(t: Throwable) {
        setEvent(RoomsEvent.LoadingEvent(false))
        updateState { copy(rooms = emptyList()) }
        CrashlyticsReporter.recordException(t)
    }

    private fun onRetrieveMessageSuccess(p: Pair<List<RoomSummary>, List<RoomWallet>>) {
        setEvent(RoomsEvent.LoadingEvent(false))
        updateState {
            copy(
                rooms = p.first.sortByLastMessage(p.second),
                roomWallets = p.second
            )
        }
    }

    fun removeRoom(roomSummary: RoomSummary) {
        viewModelScope.launch {
            setEvent(RoomsEvent.LoadingEvent(true))
            val room = getRoom(roomSummary)
            if (room != null) {
                handleRemoveRoom(room)
            } else {
                setEvent(RoomsEvent.LoadingEvent(false))
            }
        }
    }

    fun getOrCreateSupportRom() {
        viewModelScope.launch {
            setEvent(RoomsEvent.LoadingEvent(true))
            val result = getOrCreateSupportRoomUseCase(Unit)
            setEvent(RoomsEvent.LoadingEvent(false))
            if (result.isSuccess) {
                setEvent(RoomsEvent.CreateSupportRoomSuccess(result.getOrThrow().roomId))
            } else {
                setEvent(RoomsEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun handleRemoveRoom(room: Room) {
        viewModelScope.launch {
            leaveRoomUseCase.execute(room.roomId)
                .flowOn(Dispatchers.IO)
                .onException { RoomsEvent.LoadingEvent(false) }
                .collect {
                    RoomsEvent.LoadingEvent(false)
                    listenRoomSummaries()
                }
        }
    }

    fun getVisibleRooms() = getState().rooms.filter { it.shouldShow() }

    private fun getRoom(roomSummary: RoomSummary) =
        sessionHolder.getSafeActiveSession()?.roomService()?.getRoom(roomSummary.roomId)

    companion object {
        private const val TAG_SYNC = "io.nunchuk.sync"
    }
}
