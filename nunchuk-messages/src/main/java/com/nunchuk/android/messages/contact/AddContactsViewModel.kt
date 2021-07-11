package com.nunchuk.android.messages.contact

import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.messages.contact.AddContactsEvent.AllEmailValidEvent
import com.nunchuk.android.messages.contact.AddContactsEvent.InvalidEmailEvent
import com.nunchuk.android.messages.usecase.contact.AddContactUseCase
import com.nunchuk.android.utils.EmailValidator
import javax.inject.Inject

class AddContactsViewModel @Inject constructor(
    private val addContactUseCase: AddContactUseCase
) : NunchukViewModel<AddContactsState, AddContactsEvent>() {

    override val initialState: AddContactsState = AddContactsState.empty()

    fun handleAddEmail(email: String) {
        val emails = getState().emails
        if (!emails.contains(email)) {
            if (!EmailValidator.valid(email)) {
                event(InvalidEmailEvent)
            }
            (emails as MutableList).add(email)
            updateState { copy(emails = emails) }
        }
        if (emails.all(EmailValidator::valid)) {
            event(AllEmailValidEvent)
        }
    }

    fun handleRemove(email: String) {
        val emails = getState().emails
        if (emails.contains(email)) {
            (emails as MutableList).remove(email)
            updateState { copy(emails = emails) }
        }
        if (emails.all(EmailValidator::valid)) {
            event(AllEmailValidEvent)
        }
    }

    fun handleSend() {
        val emails = getState().emails
        if (emails.isNotEmpty() && emails.all(EmailValidator::valid)) {
            addContactUseCase.execute(emails)
                .defaultSchedulers()
                .subscribe({
                    android.util.Log.d("HTRAN", "add contacts error: $it")
                }, {
                    android.util.Log.e("HTRAN", "add contacts error: $it")
                })
                .addToDisposables()
        }
    }

}