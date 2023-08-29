package com.nunchuk.android.main.membership.byzantine.groupdashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.usecase.membership.CreateOrUpdateGroupChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupChatHistoryIntroViewModel @Inject constructor(
    private val createOrUpdateGroupChatUseCase: CreateOrUpdateGroupChatUseCase,
    private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {

    private val args = GroupChatHistoryIntroFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<GroupChatHistoryIntroEvent>()
    val event = _event.asSharedFlow()

    fun createGroupChat() {
        viewModelScope.launch {
            _event.emit(GroupChatHistoryIntroEvent.Loading(true))
            val result = createOrUpdateGroupChatUseCase(
                CreateOrUpdateGroupChatUseCase.Param(args.groupId)
            )
            _event.emit(GroupChatHistoryIntroEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(GroupChatHistoryIntroEvent.NavigateToGroupChat(result.getOrThrow()))
            } else {
                _event.emit(GroupChatHistoryIntroEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }
}

sealed class GroupChatHistoryIntroEvent {
    data class Error(val message: String) : GroupChatHistoryIntroEvent()
    data class Loading(val loading: Boolean) : GroupChatHistoryIntroEvent()
    data class NavigateToGroupChat(val groupChat: GroupChat) : GroupChatHistoryIntroEvent()
}