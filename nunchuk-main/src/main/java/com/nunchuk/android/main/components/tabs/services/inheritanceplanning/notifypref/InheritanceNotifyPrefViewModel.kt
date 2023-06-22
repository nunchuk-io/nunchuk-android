/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.contact.components.add.EmailWithState
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.EmailValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceNotifyPrefViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = InheritanceNotifyPrefFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<InheritanceNotifyPrefEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceNotifyPrefState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    init {
        if (args.isUpdateRequest) {
            _state.update {
                it.copy(
                    isNotify = args.preIsNotify,
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
            if (emails.isEmpty()) {
                _event.emit(InheritanceNotifyPrefEvent.EmptyEmailError)
            } else {
                if (isAllValid(emails)) {
                    _event.emit(
                        InheritanceNotifyPrefEvent.ContinueClick(
                            _state.value.emails.map { it.email },
                            _state.value.isNotify
                        )
                    )
                } else {
                    updateEmailsError()
                }
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
        } else {
            updateEmailsError()
        }
    }

    private fun updateEmailsError() {
        val emails = _state.value.emails
        val updatedEmails = emails.map {
            if (EmailValidator.valid(it.email).not()) {
                it.copy(valid = false)
            } else {
                it
            }
        }
        _state.update { it.copy(emails = updatedEmails) }
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
        emails.all { EmailValidator.valid(it.email) }
}