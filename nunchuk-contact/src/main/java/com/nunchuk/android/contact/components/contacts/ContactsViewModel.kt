package com.nunchuk.android.contact.components.contacts

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.contact.usecase.GetReceivedContactsUseCase
import com.nunchuk.android.contact.usecase.GetSentContactsUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.PAGINATION
import com.nunchuk.android.core.util.TimelineListenerAdapter
import com.nunchuk.android.messages.util.STATE_ROOM_SERVER_NOTICE
import com.nunchuk.android.messages.util.isContactUpdateEvent
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.model.SentContact
import com.nunchuk.android.share.GetContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val getSentContactsUseCase: GetSentContactsUseCase,
    private val getReceivedContactsUseCase: GetReceivedContactsUseCase,
    private val sessionHolder: SessionHolder
) : NunchukViewModel<ContactsState, Unit>() {

    override val initialState = ContactsState.empty()

    private var timeline: Timeline? = null

    private val timelineListenerAdapter = TimelineListenerAdapter()

    init {
        loadActiveSession()
        viewModelScope.launch {
            timelineListenerAdapter.data.debounce(500L).collect(::handleTimelineEvents)
        }
    }

    fun handleMatrixSignedIn() {
        loadActiveSession()
    }

    private fun loadActiveSession() {
        sessionHolder.getSafeActiveSession()?.let { session ->
            session.roomService().getRoomSummaries(roomSummaryQueryParams {
                memberships = Membership.activeMemberships()
            }).find { roomSummary ->
                roomSummary.hasTag(STATE_ROOM_SERVER_NOTICE)
            }?.let {
                session.roomService().getRoom(it.roomId)
                    ?.let(::retrieveTimelineEvents)
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

    private fun onUpdateReceivedContactRequestCount(count: Int) = updateState {
        copy(receivedContactRequestCount = count)
    }

    private fun onPendingContactError() {
        updateState { copy(pendingContacts = emptyList()) }
    }

    private fun onPendingContactSuccess(contacts: List<Contact>) {
        updateState { copy(pendingContacts = contacts) }
    }

    private fun retrieveTimelineEvents(room: Room) {
        timeline = room.timelineService()
            .createTimeline(null, TimelineSettings(initialSize = PAGINATION, true)).apply {
                removeAllListeners()
                addListener(timelineListenerAdapter)
                start()
            }
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
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