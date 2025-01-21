package com.nunchuk.android.core.groupchathistory

import com.nunchuk.android.model.HistoryPeriod

sealed class GroupChatHistoryEvent {
    data class Loading(val loading: Boolean) : GroupChatHistoryEvent()
    data class Error(val message: String) : GroupChatHistoryEvent()
    data class UpdateGroupChatSuccess(val historyPeriod: HistoryPeriod) : GroupChatHistoryEvent()
}

data class GroupChatHistoryState(
    val historyPeriods: List<HistoryPeriod> = emptyList(),
    val selectedHistoryPeriodId: String = ""
)