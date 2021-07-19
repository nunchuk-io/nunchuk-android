package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.messages.model.RoomCreationException
import com.nunchuk.android.model.Result
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import javax.inject.Inject

interface CreateRoomUseCase {
    suspend fun execute(displayName: String, userIds: List<String>): Result<Room>
}

internal class CreateRoomUseCaseImpl @Inject constructor(
) : BaseMessageUseCase(), CreateRoomUseCase {

    override suspend fun execute(displayName: String, userIds: List<String>) = exe {
        val params = CreateRoomParams().apply {
            visibility = RoomDirectoryVisibility.PRIVATE
            isDirect = false
            invitedUserIds.addAll(userIds)
            preset = CreateRoomPreset.PRESET_PRIVATE_CHAT
            name = displayName
        }
        session.getRoom(session.createRoom(params)) ?: throw RoomCreationException()
    }

}