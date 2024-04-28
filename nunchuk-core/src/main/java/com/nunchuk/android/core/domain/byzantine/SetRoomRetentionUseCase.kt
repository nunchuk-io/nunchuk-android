package com.nunchuk.android.core.domain.byzantine

import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.matrix.android.sdk.api.session.getRoom
import javax.inject.Inject
import kotlin.time.Duration

class SetRoomRetentionUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val sessionHolder: SessionHolder,
) : UseCase<SetRoomRetentionUseCase.Param, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Param) {
        sessionHolder.getSafeActiveSession()?.getRoom(parameters.roomId)?.stateService()?.sendStateEvent(
            eventType = "m.room.retention",
            stateKey = "",
            body = mapOf(
                "max_lifetime" to parameters.duration.inWholeMilliseconds,
            )
        ) ?: throw NullPointerException("Can not get room")
    }

    data class Param(val roomId: String, val duration: Duration)
}