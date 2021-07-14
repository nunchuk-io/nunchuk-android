package com.nunchuk.android.messages.contact

import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.messages.contact.AddContactsEvent.*
import com.nunchuk.android.messages.usecase.contact.AddContactUseCase
import com.nunchuk.android.utils.EmailValidator
import javax.inject.Inject

class AddContactsViewModel @Inject constructor(
    private val addContactUseCase: AddContactUseCase
) : NunchukViewModel<AddContactsState, AddContactsEvent>() {

    override val initialState: AddContactsState = AddContactsState.empty()

    fun handleAddEmail(email: String) {
        val emails = getState().emails
        if (!emails.map(EmailWithState::email).contains(email)) {
            val valid = EmailValidator.valid(email)
            if (!valid) {
                event(InvalidEmailEvent)
            }
            (emails as MutableList).add(EmailWithState(email, valid))
            updateState { copy(emails = emails) }
        }
        if (emails.all(EmailWithState::valid)) {
            event(AllEmailValidEvent)
        }
    }

    fun handleRemove(email: EmailWithState) {
        val emails = getState().emails
        (emails as MutableList).remove(email)
        updateState { copy(emails = emails) }
        if (emails.all(EmailWithState::valid)) {
            event(AllEmailValidEvent)
        }
    }

    fun handleSend() {
        val emails = getState().emails
        if (emails.isNotEmpty() && emails.all(EmailWithState::valid)) {
            addContactUseCase.execute(emails.map(EmailWithState::email))
                .defaultSchedulers()
                .subscribe({
                    if (it.isEmpty()) {
                        event(AddContactSuccessEvent)
                    } else {
                        updateEmailsError(it)
                    }
                }, {
                    event(AddContactsErrorEvent(it.message.orEmpty()))
                })
                .addToDisposables()
        }
    }

    private fun updateEmailsError(failedEmails: List<String>) {
        val emails = getState().emails
        val updatedEmails = emails.map {
            if (failedEmails.contains(it.email)) {
                it.copy(valid = false)
            } else {
                it
            }
        }
        updateState { copy(emails = updatedEmails) }
    }

    fun cleanUp() {
        updateState { AddContactsState.empty() }
    }

}