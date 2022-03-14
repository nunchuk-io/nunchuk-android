package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.retry.DefaultRetryPolicy
import com.nunchuk.android.core.retry.retryDefault
import com.nunchuk.android.messages.model.SessionLostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import javax.inject.Inject

interface GetRoomSummaryListUseCase {
    fun execute(): Flow<List<RoomSummary>>
}

internal class GetRoomSummaryListUseCaseImpl @Inject constructor(
) : GetRoomSummaryListUseCase {

    override fun execute() = flow {
        emit(
            if (SessionHolder.hasActiveSession()) {
                SessionHolder.activeSession!!.getRoomSummaries(roomSummaryQueryParams {
                    memberships = Membership.activeMemberships()
                })
            } else throw SessionLostException()
        )
    }.retryDefault(DefaultRetryPolicy(numRetries = 20, delayMillis = 500)).flowOn(Dispatchers.IO)
}
