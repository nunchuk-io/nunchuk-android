package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.messages.model.RoomCreationException
import com.nunchuk.android.model.Result
import org.matrix.android.sdk.api.session.room.Room
import javax.inject.Inject

interface CreateDirectRoomUseCase {
    suspend fun execute(userId: String): Result<Room>
}

internal class CreateDirectRoomUseCaseImpl @Inject constructor(
) : BaseMessageUseCase(), CreateDirectRoomUseCase {

    override suspend fun execute(userId: String) = exe {
        val roomId = session.getExistingDirectRoomWithUser(userId) ?: session.createDirectRoom(userId)
        session.getRoom(roomId) ?: throw RoomCreationException()
    }

}