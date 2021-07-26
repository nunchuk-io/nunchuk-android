package com.nunchuk.android.messages.components.direct

import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.util.getRoomMemberList
import com.nunchuk.android.model.Contact
import com.nunchuk.android.share.GetContactsUseCase
import org.matrix.android.sdk.api.session.room.Room
import javax.inject.Inject

class ChatInfoViewModel @Inject constructor(
    accountManager: AccountManager,
    private val getContactsUseCase: GetContactsUseCase
) : NunchukViewModel<ChatInfoState, ChatInfoEvent>() {

    private val currentId = accountManager.getAccount().chatId

    private lateinit var room: Room

    override val initialState = ChatInfoState()

    fun initialize(roomId: String) {
        SessionHolder.currentSession?.getRoom(roomId)?.let(::onRetrievedRoom) ?: event(ChatInfoEvent.RoomNotFoundEvent)
    }

    private fun onRetrievedRoom(room: Room) {
        this.room = room
        getContactsUseCase.execute()
            .defaultSchedulers()
            .subscribe(::onRetrievedContacts) {
            }
            .addToDisposables()
    }

    private fun onRetrievedContacts(contacts: List<Contact>) {
        val directUserId = room.roomSummary()?.directUserId
        if (directUserId != null) {
            getContact(contacts, directUserId)
        } else {
            val roomMemberList = room.getRoomMemberList()
            roomMemberList.firstOrNull { it.userId != currentId }?.let {
                getContact(contacts, it.userId)
            }
        }
    }

    private fun getContact(contacts: List<Contact>, directUserId: String?) {
        contacts.firstOrNull { it.chatId == directUserId }?.let { contact ->
            updateState { copy(contact = contact) }
        }
    }

}