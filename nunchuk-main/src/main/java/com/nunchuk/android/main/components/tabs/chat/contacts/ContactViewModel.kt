package com.nunchuk.android.main.components.tabs.chat.contacts

import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.messages.usecase.contact.GetContactsUseCase
import javax.inject.Inject

internal class ContactViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase
) : NunchukViewModel<ContactsState, ContactsEvent>() {

    override val initialState = ContactsState.empty()

    fun retrieveContacts() {
        getContactsUseCase.execute()
            .defaultSchedulers()
            .subscribe({
                updateState { copy(contacts = it) }
            }, {
                updateState { copy(contacts = emptyList()) }
            })
            .addToDisposables()
    }

}