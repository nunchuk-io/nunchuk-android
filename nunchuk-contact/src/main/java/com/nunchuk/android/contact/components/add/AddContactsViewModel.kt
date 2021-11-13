package com.nunchuk.android.contact.components.add

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.contact.components.add.AddContactsEvent.*
import com.nunchuk.android.contact.usecase.AddContactUseCase
import com.nunchuk.android.contact.usecase.InviteFriendUseCase
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddContactsViewModel @Inject constructor(
    private val addContactUseCase: AddContactUseCase,
    private val inviteFriendUseCase: InviteFriendUseCase
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
            viewModelScope.launch {
                addContactUseCase.execute(emails.map(EmailWithState::email))
                    .flowOn(IO)
                    .onException { onSendError(it) }
                    .flowOn(Main)
                    .collect {
                        onSendCompleted(it)
                    }
            }
        }
    }

    private fun onSendError(t: Throwable) {
        event(AddContactsErrorEvent(t.message.orEmpty()))
    }

    private fun onSendCompleted(it: List<String>) {
        if (it.isEmpty()) {
            event(AddContactSuccessEvent)
        } else {
            updateEmailsError(it)
            event(FailedSendEmailsEvent(it))
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

    fun inviteFriend(emails: List<String>) {
        if (emails.isEmpty()) {
            return
        }
        viewModelScope.launch {
            inviteFriendUseCase.execute(emails)
                .flowOn(IO)
                .onException {
                    event(InviteFriendSuccessEvent)
                }
                .flowOn(Main)
                .collect { event(InviteFriendSuccessEvent) }
        }
    }

    fun cleanUp() {
        updateState { AddContactsState.empty() }
    }

}