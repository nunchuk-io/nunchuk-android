package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.messages.model.RoomCreationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.matrix.android.sdk.api.session.room.Room
import javax.inject.Inject

interface CreateDirectRoomUseCase {
    fun execute(userId: String): Flow<Room>
}

internal class CreateDirectRoomUseCaseImpl @Inject constructor(
) : BaseMessageUseCase(), CreateDirectRoomUseCase {

    override fun execute(userId: String) = flow {
        val roomId = session.getExistingDirectRoomWithUser(userId) ?: session.createDirectRoom(userId)
        val room = session.getRoom(roomId) ?: throw RoomCreationException()
        emit(room)
        delay(CREATE_ROOM_DELAY)
    }

}