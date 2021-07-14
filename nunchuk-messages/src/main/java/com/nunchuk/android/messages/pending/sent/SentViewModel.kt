package com.nunchuk.android.messages.pending.sent

import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.messages.model.SentContact
import com.nunchuk.android.messages.repository.ContactsRepository
import com.nunchuk.android.messages.usecase.contact.GetSentContactsUseCase
import javax.inject.Inject

// FIXME
class SentViewModel @Inject constructor(
    private val getSentContactsUseCase: GetSentContactsUseCase,
    private val contactsRepository: ContactsRepository
) : NunchukViewModel<SentState, SentEvent>() {

    override val initialState: SentState = SentState()

    fun retrieveData() {
        getSentContactsUseCase.execute()
            .defaultSchedulers()
            .subscribe(
                { updateState { copy(contacts = it) } },
                { updateState { copy(contacts = emptyList()) } }
            )
            .addToDisposables()
    }

    fun handleWithDraw(contact: SentContact) {
        contactsRepository.cancelContact(contact.contact.id)
            .defaultSchedulers()
            .subscribe(::retrieveData) {}
            .addToDisposables()
    }

}