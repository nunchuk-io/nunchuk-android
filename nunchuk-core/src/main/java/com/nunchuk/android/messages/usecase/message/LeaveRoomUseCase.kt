package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.matrix.SessionHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface LeaveRoomUseCase {
    fun execute(roomId: String, reason: String? = ""): Flow<Unit>
}

internal class LeaveRoomUseCaseImpl @Inject constructor(
) : LeaveRoomUseCase {

    override fun execute(roomId: String, reason: String?) = flow {
        emit(
            if (SessionHolder.hasActiveSession()) {
                SessionHolder.activeSession!!.roomService().leaveRoom(roomId = roomId, reason = reason)
            } else Unit
        )
    }

}
