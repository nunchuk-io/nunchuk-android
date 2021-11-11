package com.nunchuk.android.messages.usecase.message

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
) : BaseMessageUseCase(), GetRoomSummaryListUseCase {

    override fun execute() = flow {
        emit(
            session.getRoomSummaries(roomSummaryQueryParams {
                memberships = Membership.activeMemberships()
            })
        )
    }
}