package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.messages.model.RoomCreationException
import com.nunchuk.android.messages.model.RoomWithTagCreationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import javax.inject.Inject

interface CreateRoomWithTagUseCase {
    fun execute(displayName: String, invitedUserIds: List<String>, tag: String): Flow<Room>
}

internal class CreateRoomWithTagUseCaseImpl @Inject constructor(
) : BaseMessageUseCase(), CreateRoomWithTagUseCase {

    override fun execute(displayName: String, invitedUserIds: List<String>, tag: String) = flow {
        val params = CreateRoomParams().apply {
            visibility = RoomDirectoryVisibility.PRIVATE
            isDirect = invitedUserIds.size == 1
            this.invitedUserIds.addAll(invitedUserIds)
            preset = CreateRoomPreset.PRESET_PRIVATE_CHAT
            name = displayName
        }
        val room = session.getRoom(session.createRoom(params))
        room?.addTag(tag, 1.0)
        emit(
            room ?: throw RoomWithTagCreationException()
        )
    }.flowOn(Dispatchers.IO)

}