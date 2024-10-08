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

package com.nunchuk.android.contact.components.contacts

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.contact.usecase.DeleteContactUseCase
import com.nunchuk.android.contact.usecase.GetReceivedContactsUseCase
import com.nunchuk.android.contact.usecase.GetSentContactsUseCase
import com.nunchuk.android.core.domain.message.HandlePushMessageUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.matrix.roomSummariesFlow
import com.nunchuk.android.core.util.PAGINATION
import com.nunchuk.android.core.util.TimelineListenerAdapter
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.messages.components.list.isServerNotices
import com.nunchuk.android.messages.util.isContactUpdateEvent
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.model.SentContact
import com.nunchuk.android.share.GetContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val getSentContactsUseCase: GetSentContactsUseCase,
    private val getReceivedContactsUseCase: GetReceivedContactsUseCase,
    private val sessionHolder: SessionHolder,
    private val handlePushMessageUseCase: HandlePushMessageUseCase,
    private val deleteContactUseCase: DeleteContactUseCase
) : NunchukViewModel<ContactsState, ContactsEvent>() {
    override val initialState = ContactsState.empty()

    private var timeline: Timeline? = null

    private val timelineListenerAdapter = TimelineListenerAdapter()

    init {
        loadActiveSession()
        viewModelScope.launch {
            timelineListenerAdapter.data.collect(::handleTimelineEvents)
        }
    }

    fun handleMatrixSignedIn() {
        timeline?.dispose()
        timeline = null
        loadActiveSession()
    }

    private var loadRoomJob: Job? = null
    private fun loadActiveSession() {
        loadRoomJob?.cancel()
        loadRoomJob = viewModelScope.launch {
            sessionHolder.getSafeActiveSession()?.let { session ->
                session.roomSummariesFlow().mapNotNull { rooms ->
                    rooms.find { it.isServerNotices() }
                }.distinctUntilChanged().collect {
                    runCatching {
                        session.roomService().joinRoom(it.roomId)
                        session.roomService().getRoom(it.roomId)
                            ?.let(::retrieveTimelineEvents)
                    }
                }
            }
        }
    }

    fun retrieveContacts() {
        viewModelScope.launch {
            getContactsUseCase.execute()
                .catch { updateState { copy(contacts = emptyList()) } }
                .collect {
                    updateState { copy(contacts = it) }
                }
        }

        viewModelScope.launch {
            val sendResultDeferred = async { getSentContactsUseCase(Unit) }
            val receivedResultDeferred = async { getReceivedContactsUseCase(Unit) }
            val sendResult = sendResultDeferred.await()
            val receivedResult = receivedResultDeferred.await()
            if (sendResult.isSuccess && receivedResult.isSuccess) {
                val sent = sendResult.getOrThrow()
                val receive = receivedResult.getOrThrow()
                onUpdateReceivedContactRequestCount(receive.size)
                onPendingContactSuccess(sent.map(SentContact::contact) + receive.map(ReceiveContact::contact))
            } else {
                onPendingContactError()
            }
        }
    }

    fun noticeRoomEvent() = timelineListenerAdapter.data

    private fun onUpdateReceivedContactRequestCount(count: Int) = updateState {
        copy(receivedContactRequestCount = count)
    }

    private fun onPendingContactError() {
        updateState { copy(pendingContacts = emptyList()) }
    }

    private fun onPendingContactSuccess(contacts: List<Contact>) {
        updateState { copy(pendingContacts = contacts) }
    }

    fun deleteContact(contact: Contact) {
        event(ContactsEvent.Loading(true))
        viewModelScope.launch {
            deleteContactUseCase(contact)
                .onSuccess {
                    val newContacts = getState().contacts.filter { it.id != contact.id }
                    updateState { copy(contacts = newContacts) }
                }
                .onFailure {
                    event(ContactsEvent.Error(it.message.orUnknownError()))
                }
            event(ContactsEvent.Loading(false))
        }
    }

    private fun retrieveTimelineEvents(room: Room) {
        if (timeline != null) return
        timeline = room.timelineService()
            .createTimeline(null, TimelineSettings(initialSize = PAGINATION, true)).apply {
                removeAllListeners()
                addListener(timelineListenerAdapter)
                start()
            }
    }

    private suspend fun handleTimelineEvents(events: List<TimelineEvent>) {
        runCatching {
            events.forEach { event ->
                handlePushMessageUseCase(event)
            }
        }
        events.findLast(TimelineEvent::isContactUpdateEvent)?.let { retrieveContacts() }
    }

    override fun onCleared() {
        timeline?.apply {
            dispose()
            removeAllListeners()
        }
        super.onCleared()
    }
}