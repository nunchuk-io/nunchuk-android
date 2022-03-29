package com.nunchuk.android.messages.components.group.members

import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.components.group.members.GroupMembersEvent.RoomNotFoundEvent
import com.nunchuk.android.messages.util.getRoomMemberList
import com.nunchuk.android.model.Contact
import com.nunchuk.android.share.GetContactsUseCase
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import javax.inject.Inject

class GroupMembersViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase
) : NunchukViewModel<GroupMembersState, GroupMembersEvent>() {

    private lateinit var room: Room

    override val initialState = GroupMembersState()

    fun initialize(roomId: String) {
        SessionHolder.activeSession?.getRoom(roomId)?.let(::onRetrievedRoom) ?: event(RoomNotFoundEvent)
    }

    private fun onRetrievedRoom(room: Room) {
        this.room = room
        val roomMemberList = room.getRoomMemberList()

        getContactsUseCase.execute()
            .defaultSchedulers()
            .subscribe({
                updateMembers(roomMemberList, it)
            }, {
                updateMembers(roomMemberList, emptyList())
            })
            .addToDisposables()
    }

    private fun updateMembers(roomMemberList: List<RoomMemberSummary>, contacts: List<Contact>) {
        updateState { copy(roomMembers = roomMemberList.map { mapMember(it, contacts) }) }
    }

    private fun mapMember(roomMember: RoomMemberSummary, contacts: List<Contact>): RoomMemberSummary {
        return (contacts.firstOrNull { it.chatId == roomMember.userId })?.let {
            roomMember.copy(userId = it.email)
        } ?: roomMember.copy(userId = "")
    }
}
