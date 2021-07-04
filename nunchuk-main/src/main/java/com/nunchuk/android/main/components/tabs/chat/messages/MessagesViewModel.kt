package com.nunchuk.android.main.components.tabs.chat.messages

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.process
import com.nunchuk.android.messages.usecase.message.GetRoomSummaryListUseCase
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

}