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

package com.nunchuk.android.contact.components.pending.sent

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.contact.components.pending.sent.SentEvent.LoadingEvent
import com.nunchuk.android.contact.usecase.CancelContactsUseCase
import com.nunchuk.android.contact.usecase.GetSentContactsUseCase
import com.nunchuk.android.model.SentContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SentViewModel @Inject constructor(
    private val getSentContactsUseCase: GetSentContactsUseCase,
    private val cancelContactsUseCase: CancelContactsUseCase,
) : NunchukViewModel<SentState, SentEvent>() {

    override val initialState: SentState = SentState()

    fun retrieveData() {
        viewModelScope.launch {
            val result = getSentContactsUseCase(Unit)
            if (result.isSuccess) {
                updateState { copy(contacts = result.getOrThrow()) }
            } else {
                updateState { copy(contacts = emptyList()) }
            }
        }
    }

    fun handleWithDraw(contact: SentContact) {
        event(LoadingEvent(true))
        viewModelScope.launch {
            event(LoadingEvent(true))
            val result = cancelContactsUseCase(contact.contact.id)
            event(LoadingEvent(false))
            if (result.isSuccess) {
                retrieveData()
            }

        }
    }
}