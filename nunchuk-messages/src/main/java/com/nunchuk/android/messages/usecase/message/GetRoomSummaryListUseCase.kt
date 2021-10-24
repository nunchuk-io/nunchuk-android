package com.nunchuk.android.messages.usecase.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.util.SYNC_EVENT_TAG
import com.nunchuk.android.messages.util.getMembersCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import javax.inject.Inject

interface GetRoomSummaryListUseCase {
    fun execute(): Flow<List<RoomSummary>>
}

internal class GetRoomSummaryListUseCaseImpl @Inject constructor(
    private val leaveRoomUseCase: LeaveRoomUseCase
) : BaseMessageUseCase(), GetRoomSummaryListUseCase {

    override fun execute() = session.getRoomSummariesLive(roomSummaryQueryParams {
        memberships = Membership.activeMemberships()
    }).asFlow().map { filterRooms(it) }

    private fun filterRooms(rooms: List<RoomSummary>): ArrayList<RoomSummary> {
        val filterRooms = ArrayList<RoomSummary>()
        rooms.forEach {
            if (it.getMembersCount() > 1 || it.hasTag(SYNC_EVENT_TAG)) {
                filterRooms.add(it)
            } else {
                leaveRoom(it)
            }
        }
        return filterRooms
    }

    private fun leaveRoom(summary: RoomSummary) {
        SessionHolder.activeSession?.getRoom(summary.roomId)?.let(leaveRoomUseCase::execute)
    }

}

@ExperimentalCoroutinesApi
fun <T> LiveData<T>.asFlow(): Flow<T> = callbackFlow {
    val observer = Observer<T> { value -> trySend(value).isSuccess }
    observeForever(observer)
    awaitClose {
        removeObserver(observer)
    }
}.flowOn(Dispatchers.Main.immediate)
