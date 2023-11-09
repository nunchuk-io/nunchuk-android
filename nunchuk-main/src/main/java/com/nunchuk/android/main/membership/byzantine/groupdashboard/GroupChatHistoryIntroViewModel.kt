package com.nunchuk.android.main.membership.byzantine.groupdashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.messages.usecase.message.CreateRoomGroupChatUseCase
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.usecase.byzantine.CreateOrUpdateGroupChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupChatHistoryIntroViewModel @Inject constructor(
    private val createRoomGroupChatUseCase: CreateRoomGroupChatUseCase,
    private val createOrUpdateGroupChatUseCase: CreateOrUpdateGroupChatUseCase,
    savedStateHandle: SavedStateHandle,
    ) : ViewModel() {

    private val args = GroupChatHistoryIntroFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<GroupChatHistoryIntroEvent>()
    val event = _event.asSharedFlow()

    fun createGroupChat() {
        viewModelScope.launch {
            _event.emit(GroupChatHistoryIntroEvent.Loading(true))
           createRoomGroupChatUseCase(
                CreateRoomGroupChatUseCase.Param(args.group, args.walletName)
            ).onSuccess {
                createOrUpdateGroupChatUseCase(
                    CreateOrUpdateGroupChatUseCase.Param(
                        groupId = args.group.id,
                        roomId = it
                    )
                ).onSuccess { groupChat ->
                    _event.emit(GroupChatHistoryIntroEvent.NavigateToGroupChat(groupChat))
                }.onFailure {
                    _event.emit(GroupChatHistoryIntroEvent.Error(it.message.orUnknownError()))
                }
            }
            _event.emit(GroupChatHistoryIntroEvent.Loading(false))
        }
    }
}

sealed class GroupChatHistoryIntroEvent {
    data class Error(val message: String) : GroupChatHistoryIntroEvent()
    data class Loading(val loading: Boolean) : GroupChatHistoryIntroEvent()
    data class NavigateToGroupChat(val groupChat: GroupChat) : GroupChatHistoryIntroEvent()
}