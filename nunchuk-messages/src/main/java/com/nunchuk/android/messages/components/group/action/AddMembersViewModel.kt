/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.messages.components.group.action

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.components.create.isContains
import com.nunchuk.android.messages.components.group.action.AddMembersEvent.AddMembersError
import com.nunchuk.android.messages.components.group.action.AddMembersEvent.AddMembersSuccessEvent
import com.nunchuk.android.messages.components.group.toMatrixError
import com.nunchuk.android.model.Contact
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import javax.inject.Inject

@HiltViewModel
class AddMembersViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val sessionHolder: SessionHolder
) : NunchukViewModel<AddMembersState, AddMembersEvent>() {

    private var contacts: List<Contact> = ArrayList()

    private lateinit var room: Room

    init {
        getContacts()
    }

    override val initialState = AddMembersState()

    fun initRoom(roomId: String) {
        room = sessionHolder.getSafeActiveSession()?.roomService()?.getRoom(roomId)!!
    }

    private fun getContacts() {
        viewModelScope.launch {
            getContactsUseCase.execute()
                .catch { }
                .collect {
                    contacts = it
                }
        }
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
        val userIds = receipts.map(Contact::chatId)

        viewModelScope.launch {
            try {
                userIds.map {
                    room.membershipService().invite(it)
                }
                event(AddMembersSuccessEvent)
            } catch (t: Throwable) {
                CrashlyticsReporter.recordException(t)
                event(AddMembersError(t.toMatrixError()))
            }
        }
    }

    fun cleanUp() {
        updateState { AddMembersState() }
    }

}