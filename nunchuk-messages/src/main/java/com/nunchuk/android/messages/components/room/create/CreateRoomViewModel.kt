package com.nunchuk.android.messages.components.room.create

import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.util.process
import com.nunchuk.android.messages.components.room.create.CreateRoomEvent.CreateRoomErrorEvent
import com.nunchuk.android.messages.components.room.create.CreateRoomEvent.CreateRoomSuccessEvent
import com.nunchuk.android.messages.usecase.message.CreateRoomUseCase
import com.nunchuk.android.model.Contact
import com.nunchuk.android.share.GetContactsUseCase
import java.util.*
import javax.inject.Inject

class CreateRoomViewModel @Inject constructor(
    accountManager: AccountManager,
    private val getContactsUseCase: GetContactsUseCase,
    private val createRoomUseCase: CreateRoomUseCase
) : NunchukViewModel<CreateRoomState, CreateRoomEvent>() {

    private val currentName: String = accountManager.getAccount().name

    private var contacts: List<Contact> = ArrayList()

    init {
        getContacts()
    }

    override val initialState = CreateRoomState.empty()

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
        val roomName = receipts.joinToString(separator = ",", transform = Contact::name) + "," + currentName
        val userIds = receipts.map(Contact::chatId)
        process({ createRoomUseCase.execute(roomName, userIds) }, {
            event(CreateRoomSuccessEvent(it.roomId))
        }, {
            event(CreateRoomErrorEvent(it.message.orEmpty()))
        })
    }

    fun cleanUp() {
        updateState { CreateRoomState.empty() }
    }

}

private fun String.isContains(word: String): Boolean {
    val locale = Locale.getDefault()
    return toLowerCase(locale).contains(word.toLowerCase(locale))
}