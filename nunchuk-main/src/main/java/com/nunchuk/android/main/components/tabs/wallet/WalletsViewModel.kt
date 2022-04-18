package com.nunchuk.android.main.components.tabs.wallet

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.matrix.roomSummariesFlow
import com.nunchuk.android.core.util.PAGINATION
import com.nunchuk.android.core.util.TimelineListenerAdapter
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
import com.nunchuk.android.messages.util.isDisplayable
import com.nunchuk.android.messages.util.isNunchukEvent
import com.nunchuk.android.messages.util.toNunchukMatrixEvent
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.usecase.ConsumeEventUseCase
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import timber.log.Timber
import javax.inject.Inject

internal class WalletsViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val consumeEventUseCase: ConsumeEventUseCase
) : NunchukViewModel<WalletsState, WalletsEvent>() {

    private lateinit var timeline: Timeline
    private var timelineListenerAdapter = TimelineListenerAdapter(::handleTimelineEvents)

    override val initialState = WalletsState()

    init {
        // NUN-1349
        SessionHolder.activeSession?.let(::listenRoomSummaries)
    }
    fun getAppSettings() {
        viewModelScope.launch {
            getAppSettingUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(chain = it.chain)
                    }
                }
        }
    }

    fun retrieveData() {
        viewModelScope.launch {
            getCompoundSignersUseCase.execute()
                .zip(getWalletsUseCase.execute()) { p, wallets ->
                    Triple(p.first, p.second, wallets)
                }
                .flowOn(Dispatchers.IO)
                .onException {
                    updateState { copy(signers = emptyList(), masterSigners = emptyList()) }
                }
                .flowOn(Dispatchers.Main)
                .onCompletion {
                    event(Loading(false))
                }
                .collect { updateState { copy(masterSigners = it.first, signers = it.second, wallets = it.third) } }
        }
    }

    private fun listenRoomSummaries(session: Session) {
        session.roomSummariesFlow()
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
            .onStart { }
            .onEach { roomSummaries ->
                roomSummaries.forEach { roomSummary ->
                    if ((roomSummary.joinedMembersCount ?: 0) > 1) {
                        SessionHolder.activeSession?.getRoom(roomSummary.roomId)?.let { room ->
                            retrieveTimelineEvents(room)
                        }
                    }
                }
            }
            .onCompletion {  }
            .flowOn(Dispatchers.Main)
            .launchIn(viewModelScope)
    }

    private fun retrieveTimelineEvents(room: Room) {
        timeline = room.createTimeline(null, TimelineSettings(initialSize = PAGINATION, true))
        timeline.removeListener(timelineListenerAdapter)
        timeline.addListener(timelineListenerAdapter)
        timeline.start()
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
        val displayableEvents = events.filter(TimelineEvent::isDisplayable)
        val nunchukEvents = displayableEvents.filter(TimelineEvent::isNunchukEvent)
        viewModelScope.launch {
            val sortedEvents = nunchukEvents.map(TimelineEvent::toNunchukMatrixEvent)
                .sortedBy(NunchukMatrixEvent::time)
            consumeEvents(sortedEvents)
        }
    }

    private fun consumeEvents(
        sortedEvents: List<NunchukMatrixEvent>
    ) {
        viewModelScope.launch {
            consumeEventUseCase.execute(sortedEvents)
                .flowOn(Dispatchers.IO)
                .onException {}
                .flowOn(Dispatchers.Main)
                .onCompletion {}
                .collect { Timber.d("Consume event completed") }
        }
    }

    fun handleAddSignerOrWallet() {
        if (hasSigner()) {
            handleAddWallet()
        } else {
            handleAddSigner()
        }
    }

    fun handleAddSigner() {
        event(ShowSignerIntroEvent)
    }

    fun handleAddWallet() {
        event(AddWalletEvent)
    }

    fun hasSigner() = getState().signers.isNotEmpty() || getState().masterSigners.isNotEmpty()

}