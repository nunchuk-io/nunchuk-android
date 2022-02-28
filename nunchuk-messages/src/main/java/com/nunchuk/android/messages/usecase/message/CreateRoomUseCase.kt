package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.messages.model.RoomCreationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import javax.inject.Inject

interface CreateRoomUseCase {
    fun execute(displayName: String, invitedUserIds: List<String>): Flow<Room>
}

internal class CreateRoomUseCaseImpl @Inject constructor(
) : BaseMessageUseCase(), CreateRoomUseCase {

    override fun execute(displayName: String, invitedUserIds: List<String>) = flow {
        val params = CreateRoomParams().apply {
            visibility = RoomDirectoryVisibility.PRIVATE
            isDirect = invitedUserIds.size == 1
            this.invitedUserIds.addAll(invitedUserIds)
            preset = CreateRoomPreset.PRESET_PRIVATE_CHAT
            name = displayName
        }
        emit(
            session.getRoom(session.createRoom(params)) ?: throw RoomCreationException()
        )
        delay(CREATE_ROOM_DELAY)
    }

}

internal const val CREATE_ROOM_DELAY = 1000L