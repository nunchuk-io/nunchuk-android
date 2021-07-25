package com.nunchuk.android.contact.components.pending.receive

import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.contact.components.pending.receive.ReceivedEvent.LoadingEvent
import com.nunchuk.android.contact.repository.ContactsRepository
import com.nunchuk.android.contact.usecase.GetReceivedContactsUseCase
import com.nunchuk.android.model.ReceiveContact
import javax.inject.Inject

// FIXME
class ReceivedViewModel @Inject constructor(
    private val getReceivedContactsUseCase: GetReceivedContactsUseCase,
    private val contactsRepository: ContactsRepository,
) : NunchukViewModel<ReceivedState, ReceivedEvent>() {

    override val initialState: ReceivedState = ReceivedState()

    fun retrieveData() {
        getReceivedContactsUseCase.execute()
            .defaultSchedulers()
            .subscribe(
                { updateState { copy(contacts = it) } },
                { updateState { copy(contacts = emptyList()) } }
            )
            .addToDisposables()
    }

    fun handleAcceptRequest(contact: ReceiveContact) {
        event(LoadingEvent(true))
        contactsRepository.acceptContact(contact.contact.id)
            .defaultSchedulers()
            .doAfterTerminate { event(LoadingEvent(false)) }
            .subscribe(::retrieveData) {}
            .addToDisposables()
    }

    fun handleCancelRequest(contact: ReceiveContact) {
        event(LoadingEvent(true))
        contactsRepository.cancelContact(contact.contact.id)
            .defaultSchedulers()
            .doAfterTerminate { event(LoadingEvent(false)) }
            .subscribe(::retrieveData) {}
            .addToDisposables()
    }

}