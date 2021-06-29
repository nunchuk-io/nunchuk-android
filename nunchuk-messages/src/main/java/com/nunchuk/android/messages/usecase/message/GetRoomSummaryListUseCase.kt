package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.model.Result
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import javax.inject.Inject

interface GetRoomSummaryListUseCase {
    suspend fun execute(): Result<List<RoomSummary>>
}

internal class GetRoomSummaryListUseCaseImpl @Inject constructor(
) : BaseMessageUseCase(), GetRoomSummaryListUseCase {

    override suspend fun execute() = exe {
        session.getRoomSummaries(roomSummaryQueryParams {
            memberships = Membership.activeMemberships()
        })
    }

}