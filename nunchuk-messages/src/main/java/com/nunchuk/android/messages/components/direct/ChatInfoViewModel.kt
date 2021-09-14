package com.nunchuk.android.messages.components.direct

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.util.getRoomMemberList
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.usecase.GetRoomWalletUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.session.room.Room
import timber.log.Timber
import javax.inject.Inject

class ChatInfoViewModel @Inject constructor(
    accountManager: AccountManager,
    private val getContactsUseCase: GetContactsUseCase,
    private val getRoomWalletUseCase: GetRoomWalletUseCase
) : NunchukViewModel<ChatInfoState, ChatInfoEvent>() {

    private val currentId = accountManager.getAccount().chatId

    private lateinit var room: Room

    override val initialState = ChatInfoState()

    fun initialize(roomId: String) {
        SessionHolder.activeSession?.getRoom(roomId)?.let(::onRetrievedRoom) ?: event(ChatInfoEvent.RoomNotFoundEvent)
    }

    private fun onRetrievedRoom(room: Room) {
        this.room = room
        getContactsUseCase.execute()
            .defaultSchedulers()
            .subscribe(::onRetrievedContacts) {
            }
            .addToDisposables()
        getRoomWallet()
    }

    private fun getRoomWallet() {
        getRoomWalletUseCase.execute(roomId = room.roomId)
            .catch { Timber.e("get room failed:$it") }
            .onEach { onGetRoomWallet(it) }
            .launchIn(viewModelScope)
    }

    private fun onGetRoomWallet(roomWallet: RoomWallet) {
        updateState { copy(roomWallet = roomWallet) }
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