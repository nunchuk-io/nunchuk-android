package com.nunchuk.android.messages.components.list

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.components.list.RoomsEvent.LoadingEvent
import com.nunchuk.android.messages.usecase.message.GetRoomSummaryListUseCase
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.messages.util.sortByLastMessage
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.usecase.GetAllRoomWalletsUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import io.reactivex.Completable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RoomsViewModel @Inject constructor(
    private val getAllRoomWalletsUseCase: GetAllRoomWalletsUseCase,
    private val getRoomSummaryListUseCase: GetRoomSummaryListUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase
) : NunchukViewModel<RoomsState, RoomsEvent>() {

    override val initialState = RoomsState.empty()

    init {
        SessionHolder.activeSession?.let(::subscribeEvent)
    }

    private fun subscribeEvent(session: Session) {
        session.addListener(object : Session.Listener {
            override fun onNewInvitedRoom(session: Session, roomId: String) {
                session.getRoom(roomId)?.let(::joinRoom)
                viewModelScope.launch {
                    getRoomSummaryListUseCase.execute()
                        .onException { }
                        .collect { updateState { copy(rooms = it) } }
                }
            }
        })
    }

    private fun joinRoom(room: Room) {
        viewModelScope.launch {
            try {
                room.join()
            } catch (e: Throwable) {
                CrashlyticsReporter.recordException(e)
            }
        }
    }

    fun retrieveMessages() {
        viewModelScope.launch {
            getRoomSummaryListUseCase.execute()
                .zip(getAllRoomWalletsUseCase.execute()) { rooms, wallets -> rooms to wallets }
                .flowOn(IO)
                .onException { onRetrieveMessageError(it) }
                .flowOn(Dispatchers.Main)
                .collect { onRetrieveMessageSuccess(it) }
        }
    }

    private fun onRetrieveMessageError(t: Throwable) {
        event(LoadingEvent(false))
        updateState { copy(rooms = emptyList()) }
        CrashlyticsReporter.recordException(t)
    }

    private fun onRetrieveMessageSuccess(p: Pair<List<RoomSummary>, List<RoomWallet>>) {
        event(LoadingEvent(false))
        updateState { copy(rooms = p.first.sortByLastMessage(), roomWallets = p.second) }
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
        viewModelScope.launch {
            leaveRoomUseCase.execute(room)
                .flowOn(IO)
                .onException { LoadingEvent(false) }
                .collect { awaitAndRetrieveMessages() }
        }
    }

    private fun awaitAndRetrieveMessages() {
        Completable.fromCallable {}
            .delay(DELAY_IN_SECONDS, TimeUnit.SECONDS)
            .defaultSchedulers()
            .doAfterTerminate { event(LoadingEvent(false)) }
            .subscribe(::retrieveMessages, CrashlyticsReporter::recordException)
            .addToDisposables()
    }

    private fun getRoom(roomSummary: RoomSummary) = SessionHolder.activeSession?.getRoom(roomSummary.roomId)

    companion object {
        private const val DELAY_IN_SECONDS = 2L
    }

}