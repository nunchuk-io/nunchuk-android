package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.contact.components.add.EmailWithState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceNotifyPrefViewModel @Inject constructor(savedStateHandle: SavedStateHandle) :
    ViewModel() {

    private val args = InheritanceNotifyPrefFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<InheritanceNotifyPrefEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceNotifyPrefState())
    val state = _state.asStateFlow()

    init {
        if (args.isUpdateRequest) {
            _state.update {
                it.copy(isNotify = args.preIsNotify,
                    emails = args.preEmails?.map { email ->
                        EmailWithState(
                            email = email,
                            valid = true
                        )
                    }.orEmpty()
                )
            }
        }
    }

    fun onContinueClicked() =
        viewModelScope.launch {
            val emails = _state.value.emails
            if (isAllValid(emails)) {
                _event.emit(
                    InheritanceNotifyPrefEvent.ContinueClick(
                        _state.value.emails.map { it.email },
                        _state.value.isNotify
                    )
                )
            }
        }

    fun updateIsNotify(isNotify: Boolean) {
        _state.update { it.copy(isNotify = isNotify) }
    }

    fun handleAddEmail(email: String) = viewModelScope.launch {
        val newEmails = _state.value.emails.toMutableList()
        if (!newEmails.map(EmailWithState::email).contains(email)) {
            newEmails.add(EmailWithState(email, email.trim().isNotEmpty()))
            _state.update { it.copy(emails = newEmails) }
        }
        if (isAllValid(newEmails)) {
            _event.emit(InheritanceNotifyPrefEvent.AllEmailValidEvent)
        }
    }

    fun handleRemove(email: EmailWithState) = viewModelScope.launch {
        val newEmails = _state.value.emails.toMutableList()
        newEmails.remove(email)
        _state.update { it.copy(emails = newEmails) }
        if (isAllValid(newEmails)) {
            _event.emit(InheritanceNotifyPrefEvent.AllEmailValidEvent)
        }
    }

    private fun isAllValid(emails: List<EmailWithState>) =
        emails.all { it.email.trim().isNotEmpty() }
}