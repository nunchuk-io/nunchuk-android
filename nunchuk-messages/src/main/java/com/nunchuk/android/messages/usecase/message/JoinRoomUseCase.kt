package com.nunchuk.android.messages.usecase.message

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface JoinRoomUseCase {
    fun execute(
        roomIdOrAlias: String,
        reason: String? = null,
        viaServers: List<String> = emptyList()
    ): Flow<Unit>
}

internal class JoinRoomUseCaseImpl @Inject constructor(
) : BaseMessageUseCase(), JoinRoomUseCase {

    override fun execute(
        roomIdOrAlias: String,
        reason: String?,
        viaServers: List<String>
    ) = flow {
        emit(session.joinRoom(roomIdOrAlias, reason, viaServers))
    }

}