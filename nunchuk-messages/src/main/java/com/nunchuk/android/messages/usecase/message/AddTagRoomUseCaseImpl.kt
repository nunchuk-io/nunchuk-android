package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.messages.model.RoomNotFoundException
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface AddTagRoomUseCase {
    fun execute(tagName: String, roomId: String): Flow<Unit>
}

internal class AddTagRoomUseCaseImpl @Inject constructor(
) : BaseMessageUseCase(), AddTagRoomUseCase {

    override fun execute(tagName: String, roomId: String) = flow {
        emit(
            session.getRoom(roomId)?.addTag(tagName, 1.0) ?: throw RoomNotFoundException(roomId)
        )
    }.catch { CrashlyticsReporter.recordException(it) }
}