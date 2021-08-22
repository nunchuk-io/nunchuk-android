package com.nunchuk.android.messages.components.list

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.process
import com.nunchuk.android.messages.components.list.RoomsEvent.LoadingEvent
import com.nunchuk.android.messages.usecase.message.GetRoomSummaryListUseCase
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.messages.util.sortByLastMessage
import io.reactivex.Completable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RoomsViewModel @Inject constructor(
    private val getRoomSummaryListUseCase: GetRoomSummaryListUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase
) : NunchukViewModel<RoomsState, RoomsEvent>() {

    override val initialState = RoomsState.empty()

    fun retrieveMessages() {
        viewModelScope.launch {
            getRoomSummaryListUseCase.execute()
                .flowOn(Dispatchers.IO)
                .catch { onRetrieveMessageError() }
                .flowOn(Dispatchers.Main)
                .collect { onRetrieveMessageSuccess(it) }
        }
    }

    private fun onRetrieveMessageError() {
        event(LoadingEvent(false))
        updateState { copy(rooms = emptyList()) }
    }

    private fun onRetrieveMessageSuccess(roomSummaryList: List<RoomSummary>) {
        event(LoadingEvent(false))
        updateState { copy(rooms = roomSummaryList.sortByLastMessage()) }
    }

    fun removeRoom(roomSummary: RoomSummary) {
        viewModelScope.launch {
            event(LoadingEvent(true))
            val room = getRoom(roomSummary)
            if (room != null) {
                handleRemoveRoom(room)
            } else {
                event(LoadingEvent(false))
            }
        }
    }

    private fun handleRemoveRoom(room: Room) {
        process({ leaveRoomUseCase.execute(room) }, {
            awaitAndRetrieveMessages()
        }, {
            event(LoadingEvent(false))
        })

    }

    private fun awaitAndRetrieveMessages() {
        Completable.fromCallable {}
            .delay(DELAY_IN_SECONDS, TimeUnit.SECONDS)
            .defaultSchedulers()
            .subscribe(::retrieveMessages) {
                event(LoadingEvent(false))
            }
            .addToDisposables()
    }

    private fun getRoom(roomSummary: RoomSummary) = SessionHolder.currentSession?.getRoom(roomSummary.roomId)

    companion object {
        private const val DELAY_IN_SECONDS = 2L
    }

}