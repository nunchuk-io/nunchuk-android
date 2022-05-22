package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.matrix.SessionHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.matrix.android.sdk.api.session.room.Room
import javax.inject.Inject

interface LeaveRoomUseCase {
    fun execute(room: Room, reason: String? = ""): Flow<Unit>
}

internal class LeaveRoomUseCaseImpl @Inject constructor(
) : LeaveRoomUseCase {

    override fun execute(room: Room, reason: String?) = flow {
        emit(
            if (SessionHolder.hasActiveSession()) {
                SessionHolder.activeSession!!.leaveRoom(roomId = room.roomId, reason = reason)
            } else Unit
        )
    }

}
