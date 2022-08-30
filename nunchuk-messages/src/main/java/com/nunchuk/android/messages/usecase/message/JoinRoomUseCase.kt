package com.nunchuk.android.messages.usecase.message

import com.nunchuk.android.core.matrix.SessionHolder
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
    sessionHolder: SessionHolder
) : BaseMessageUseCase(sessionHolder), JoinRoomUseCase {

    override fun execute(
        roomIdOrAlias: String,
        reason: String?,
        viaServers: List<String>
    ) = flow {
        emit(session.roomService().joinRoom(roomIdOrAlias, reason, viaServers))
    }

}