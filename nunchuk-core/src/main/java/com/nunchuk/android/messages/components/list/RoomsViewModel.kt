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

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.core.domain.settings.MarkSyncRoomSuccessUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.matrix.roomSummariesFlow
import com.nunchuk.android.core.util.SUPPORT_ROOM_TYPE
import com.nunchuk.android.core.util.SUPPORT_TEST_NET_ROOM_TYPE
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.listener.GroupDeleteListener
import com.nunchuk.android.listener.GroupMessageListener
import com.nunchuk.android.log.fileLog
import com.nunchuk.android.messages.usecase.message.GetGroupChatRoomsUseCase
import com.nunchuk.android.messages.usecase.message.GetGroupMessageAccountUseCase
import com.nunchuk.android.messages.usecase.message.GetOrCreateSupportRoomUseCase
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.messages.util.sortByLastMessage
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.usecase.GetAllRoomWalletsUseCase
import com.nunchuk.android.usecase.membership.DeleteGroupChatUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
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
    private val markSyncRoomSuccessUseCase: MarkSyncRoomSuccessUseCase,
    private val deleteGroupChatUseCase: DeleteGroupChatUseCase,
    private val getGroupChatRoomsUseCase: GetGroupChatRoomsUseCase,
    getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
    private val getGroupMessageAccountUseCase: GetGroupMessageAccountUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : NunchukViewModel<RoomsState, RoomsEvent>() {

    override val initialState = RoomsState.empty()

    val plans = getLocalMembershipPlansFlowUseCase(Unit)
        .map { it.getOrElse { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var listenJob: Job? = null

    init {
        listenRoomSummaries()
        getGroupMessageAccount()
        listenGroupMessages()
    }

    private fun listenGroupMessages() {
        viewModelScope.launch {
            GroupMessageListener.getMessageFlow().collect { message ->
                if (getState().groupWalletWalletIds.contains(message.walletId)) {
                    updateState {
                        copy(
                            groupWalletMessages = getState().groupWalletMessages.map {
                                if (it.walletId == message.walletId) {
                                    it.copy(
                                        id = message.id,
                                        content = message.content,
                                        timestamp = message.timestamp * 1000,
                                    )
                                } else {
                                    it
                                }
                            }
                        )
                    }
                    combineRooms()
                } else {
                    getGroupMessageAccount()
                }
            }
        }
        viewModelScope.launch {
            GroupDeleteListener.groupDeleteFlow.collect { _ ->
                getGroupMessageAccount()
            }
        }
    }

    fun getGroupMessageAccount() {
        viewModelScope.launch {
            delay(500)
            getGroupMessageAccountUseCase(Unit)
                .onSuccess { messages ->
                    updateState {
                        copy(
                            groupWalletMessages = messages,
                            groupWalletWalletIds = messages.map { it.walletId }.toSet()
                        )
                    }
                    combineRooms()
                }
        }
    }

    fun handleMatrixSignedIn() {
        listenRoomSummaries()
    }

    fun listenRoomSummaries() {
        listenJob?.cancel()
        val session = sessionHolder.getSafeActiveSession() ?: return
        listenJob = session.roomSummariesFlow()
            .flowOn(dispatcher)
            .distinctUntilChanged()
            .onStart { setEvent(RoomsEvent.LoadingEvent(true)) }
            .onEach {
                fileLog("listenRoomSummaries($it)")
                leaveDraftSyncRoom(it)
                retrieveMessages(it)
                if (it.isNotEmpty()) {
                    markSyncRoomSuccessUseCase(Unit)
                }
                listenRoomEvents(it)
            }
            .onCompletion { setEvent(RoomsEvent.LoadingEvent(false)) }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
    }

    private suspend fun listenRoomEvents(rooms: List<RoomSummary>) {
        val roomIds = rooms.map { it.roomId }
        getGroupChatRoomsUseCase(GetGroupChatRoomsUseCase.Params(roomIds))
            .onSuccess { result ->
                updateState {
                    copy(
                        groupChatRooms = result.associateBy { it.roomId }.toMutableMap()
                    )
                }
            }
    }

    private suspend fun retrieveMessages(rooms: List<RoomSummary>) {
        getAllRoomWalletsUseCase.execute()
            .map { wallets -> rooms to wallets }
            .flowOn(dispatcher)
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
        viewModelScope.launch(dispatcher) {
            val draftSyncRooms = summaries.filter {
                it.displayName == TAG_SYNC && it.tags.isEmpty()
                        || ((it.roomType == SUPPORT_ROOM_TYPE || it.roomType == SUPPORT_TEST_NET_ROOM_TYPE)
                        && it.joinedMembersCount == 1
                        && it.latestPreviewableEvent != null)
            }
            draftSyncRooms.forEach {
                leaveRoomUseCase.execute(it.roomId)
                    .flowOn(dispatcher)
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
        val rooms = p.first.sortByLastMessage(p.second)
        updateState {
            copy(
                matrixRooms = rooms,
                roomWallets = p.second
            )
        }
        combineRooms()
    }

    fun removeRoom(roomSummary: RoomSummary) {
        viewModelScope.launch {
            setEvent(RoomsEvent.LoadingEvent(true))
            val room = getRoom(roomSummary)
            if (room != null) {
                if (getState().groupChatRooms.containsKey(room.roomId)) {
                    deleteGroupChatUseCase(getState().groupChatRooms[room.roomId]!!.groupId)
                        .onSuccess {
                            setEvent(RoomsEvent.RemoveRoomSuccess(roomSummary))
                            RoomsEvent.LoadingEvent(false)
                            listenRoomSummaries()
                        }.onFailure { throwable ->
                            setEvent(RoomsEvent.LoadingEvent(false))
                            setEvent(RoomsEvent.ShowError(throwable.message.orUnknownError()))
                        }
                } else {
                    handleRemoveRoom(room, roomSummary)
                }
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

    private fun handleRemoveRoom(room: Room, roomSummary: RoomSummary) {
        viewModelScope.launch {
            leaveRoomUseCase.execute(room.roomId)
                .flowOn(dispatcher)
                .onException { RoomsEvent.LoadingEvent(false) }
                .collect {
                    setEvent(RoomsEvent.RemoveRoomSuccess(roomSummary))
                    RoomsEvent.LoadingEvent(false)
                    listenRoomSummaries()
                }
        }
    }

    fun getVisibleRooms() =
        getState().rooms.filter { it is RoomMessage.GroupWalletRoom || ((it as? RoomMessage.MatrixRoom)?.data?.shouldShow() == true) }

    private fun getRoom(roomSummary: RoomSummary) =
        sessionHolder.getSafeActiveSession()?.roomService()?.getRoom(roomSummary.roomId)

    private fun combineRooms() {
        val result = mutableListOf<RoomMessage>()
        val groupWalletRooms = getState().groupWalletMessages.sortedByDescending { it.timestamp }
        val matrixRooms = getState().matrixRooms
        result.addAll(groupWalletRooms.map { RoomMessage.GroupWalletRoom(it) } + matrixRooms.map {
            RoomMessage.MatrixRoom(
                it
            )
        })
        updateState { copy(rooms = result) }
    }

    companion object {
        private const val TAG_SYNC = "io.nunchuk.sync"
    }
}
