package com.nunchuk.android.main.components.tabs.chat.contacts

import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.contact.usecase.GetReceivedContactsUseCase
import com.nunchuk.android.contact.usecase.GetSentContactsUseCase
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.model.SentContact
import com.nunchuk.android.share.GetContactsUseCase
import io.reactivex.Single
import javax.inject.Inject

internal class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val getSentContactsUseCase: GetSentContactsUseCase,
    private val getReceivedContactsUseCase: GetReceivedContactsUseCase
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

        Single.zip(
            getSentContactsUseCase.execute(),
            getReceivedContactsUseCase.execute()
        ) { sent, receive -> sent.map(SentContact::contact) + receive.map(ReceiveContact::contact) }
            .defaultSchedulers()
            .subscribe(::onPendingContactSuccess, ::onPendingContactError)
            .addToDisposables()
    }

    private fun onPendingContactError(throwable: Throwable) {
        updateState { copy(pendingContacts = emptyList()) }
    }

    private fun onPendingContactSuccess(contacts: List<Contact>) {
        updateState { copy(pendingContacts = contacts) }
    }

}