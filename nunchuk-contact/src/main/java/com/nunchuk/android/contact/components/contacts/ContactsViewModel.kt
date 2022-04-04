package com.nunchuk.android.contact.components.contacts

import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.contact.usecase.GetReceivedContactsUseCase
import com.nunchuk.android.contact.usecase.GetSentContactsUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.PAGINATION
import com.nunchuk.android.core.util.TimelineListenerAdapter
import com.nunchuk.android.messages.util.STATE_ROOM_SERVER_NOTICE
import com.nunchuk.android.messages.util.isContactRequestEvent
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.model.SentContact
import com.nunchuk.android.share.GetContactsUseCase
import io.reactivex.Single
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import javax.inject.Inject

class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val getSentContactsUseCase: GetSentContactsUseCase,
    private val getReceivedContactsUseCase: GetReceivedContactsUseCase
) : NunchukViewModel<ContactsState, Unit>() {

    override val initialState = ContactsState.empty()

    private lateinit var timeline: Timeline

    fun registerNewContactRequestEvent() {
        SessionHolder.activeSession?.getRoomSummaries(roomSummaryQueryParams {
            memberships = Membership.activeMemberships()
        })?.find { roomSummary ->
            roomSummary.hasTag(STATE_ROOM_SERVER_NOTICE)
        }?.let {
            SessionHolder.activeSession?.getRoom(it.roomId)?.let(::retrieveTimelineEvents)
        }
    }

    fun retrieveContacts() {
        getContactsUseCase.execute()
            .defaultSchedulers()
            .subscribe({
                updateState { copy(contacts = it) }
            }, {
                updateState { copy(contacts = emptyList()) }
            })
            .addToDisposables()

        Single.zip(
            getSentContactsUseCase.execute(),
            getReceivedContactsUseCase.execute()
        ) { sent, receive -> sent.map(SentContact::contact) + receive.map(ReceiveContact::contact) }
            .defaultSchedulers()
            .subscribe(::onPendingContactSuccess) { onPendingContactError() }
            .addToDisposables()
    }

    private fun onPendingContactError() {
        updateState { copy(pendingContacts = emptyList()) }
    }

    private fun onPendingContactSuccess(contacts: List<Contact>) {
        updateState { copy(pendingContacts = contacts) }
    }

    private fun retrieveTimelineEvents(room: Room) {
        timeline = room.createTimeline(null, TimelineSettings(initialSize = PAGINATION, true))
        timeline.removeAllListeners()
        timeline.addListener(TimelineListenerAdapter(::handleTimelineEvents))
        timeline.start()
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
        events.findLast(TimelineEvent::isContactRequestEvent)?.let { retrieveContacts() }
    }

}