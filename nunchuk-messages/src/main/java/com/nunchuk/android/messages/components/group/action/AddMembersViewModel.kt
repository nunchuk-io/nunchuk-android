package com.nunchuk.android.messages.components.group.action

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.components.create.isContains
import com.nunchuk.android.messages.components.group.action.AddMembersEvent.AddMembersError
import com.nunchuk.android.messages.components.group.action.AddMembersEvent.AddMembersSuccessEvent
import com.nunchuk.android.messages.components.group.toMatrixError
import com.nunchuk.android.model.Contact
import com.nunchuk.android.share.GetContactsUseCase
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import java.util.*
import javax.inject.Inject

class AddMembersViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase
) : NunchukViewModel<AddMembersState, AddMembersEvent>() {

    private var contacts: List<Contact> = ArrayList()

    private lateinit var room: Room

    init {
        getContacts()
    }

    override val initialState = AddMembersState()

    fun initRoom(roomId: String) {
        room = SessionHolder.activeSession?.getRoom(roomId)!!
    }

    private fun getContacts() {
        getContactsUseCase.execute()
            .defaultSchedulers()
            .subscribe({
                contacts = it
            }, {
            })
            .addToDisposables()
    }

    fun handleInput(word: String) {
        val suggestions = contacts.filter { it.name.isContains(word) || it.email.isContains(word) }
        updateState {
            copy(suggestions = suggestions)
        }
    }

    fun handleSelectContact(contact: Contact) {
        val receipts = getState().receipts
        if (!receipts.contains(contact)) {
            (receipts as MutableList).add(contact)
            updateState { copy(receipts = receipts) }
        }
    }

    fun handleRemove(contact: Contact) {
        val receipts = getState().receipts
        if (receipts.contains(contact)) {
            (receipts as MutableList).remove(contact)
            updateState { copy(receipts = receipts) }
        }
    }

    fun handleDone() {
        val receipts = getState().receipts
        val userIds = receipts.map(Contact::chatId)

        viewModelScope.launch {
            try {
                userIds.map {
                    room.invite(it)
                }
                event(AddMembersSuccessEvent)
            } catch (t: Throwable) {
                event(AddMembersError(t.toMatrixError()))
            }
        }
    }

    fun cleanUp() {
        updateState { AddMembersState() }
    }

}