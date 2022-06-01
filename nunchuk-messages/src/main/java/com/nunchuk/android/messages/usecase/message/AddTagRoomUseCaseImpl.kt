package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.messages.model.RoomNotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface AddTagRoomUseCase {
    fun execute(tagName: String, roomId: String): Flow<Unit>
}

internal class AddTagRoomUseCaseImpl @Inject constructor(
) : BaseMessageUseCase(), AddTagRoomUseCase {

    override fun execute(tagName: String, roomId: String) = flow {
        emit(
            session.roomService().getRoom(roomId)?.tagsService()?.addTag(tagName, 1.0) ?: throw RoomNotFoundException(roomId)
        )
    }
}