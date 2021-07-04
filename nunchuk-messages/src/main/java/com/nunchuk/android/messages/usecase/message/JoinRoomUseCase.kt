package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.model.Result
import javax.inject.Inject

interface JoinRoomUseCase {
    suspend fun execute(
        roomIdOrAlias: String,
        reason: String? = null,
        viaServers: List<String> = emptyList()
    ): Result<Unit>
}

internal class JoinRoomUseCaseImpl @Inject constructor(
) : BaseMessageUseCase(), JoinRoomUseCase {

    override suspend fun execute(
        roomIdOrAlias: String,
        reason: String?,
        viaServers: List<String>
    ) = exe {
        session.joinRoom(roomIdOrAlias, reason, viaServers)
    }

}