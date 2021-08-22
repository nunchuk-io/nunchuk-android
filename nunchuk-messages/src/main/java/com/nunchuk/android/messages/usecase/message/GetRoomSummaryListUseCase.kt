package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.util.getMembersCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

    override fun execute() = flow {
        emit(
            session.getRoomSummaries(roomSummaryQueryParams {
                memberships = Membership.activeMemberships()
            }).flatMap {
                val filterRooms = ArrayList<RoomSummary>()
                if (it.getMembersCount() > 1) {
                    filterRooms.add(it)
                } else {
                    try {
                        leaveRoom(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                filterRooms
            }
        )
    }

    private suspend fun leaveRoom(summary: RoomSummary) {
        SessionHolder.currentSession?.getRoom(summary.roomId)?.let {
            leaveRoomUseCase.execute(it)
        }
    }

}