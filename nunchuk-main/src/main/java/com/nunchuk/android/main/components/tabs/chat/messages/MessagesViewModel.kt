package com.nunchuk.android.main.components.tabs.chat.messages

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.process
import com.nunchuk.android.messages.usecase.message.GetRoomSummaryListUseCase
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

internal class MessagesViewModel @Inject constructor(
    private val getRoomSummaryListUseCase: GetRoomSummaryListUseCase
) : NunchukViewModel<MessagesState, MessagesEvent>() {

    override val initialState = MessagesState.empty()

    fun retrieveMessages() {
        process(getRoomSummaryListUseCase::execute, {
            updateState { copy(rooms = it) }
        }, {
            updateState { copy(rooms = emptyList()) }
        })
    }

    fun removeRoom(roomSummary: RoomSummary) {
        viewModelScope.launch {
            SessionHolder.currentSession?.getRoom(roomSummary.roomId)?.apply {
                try {
                    leave()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                retrieveMessages()
            }
        }
    }

}