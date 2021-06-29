package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.model.Result
import org.matrix.android.sdk.api.session.room.Room
import javax.inject.Inject

interface LeaveRoomUseCase {
    suspend fun execute(room: Room, reason: String? = ""): Result<Unit>
}

internal class LeaveRoomUseCaseImpl @Inject constructor(
) : BaseMessageUseCase(), LeaveRoomUseCase {

    override suspend fun execute(room: Room, reason: String?) = exe {
        room.leave(reason)
    }

}