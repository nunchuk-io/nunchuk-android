package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.network.UnauthorizedEventBus
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.statistics.StatisticEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionListener @Inject constructor(
) : Session.Listener {
    override fun onGlobalError(session: Session, globalError: GlobalError) {
        if (globalError is GlobalError.InvalidToken || globalError === GlobalError.ExpiredAccount) {
            UnauthorizedEventBus.instance().publish()
        }
    }

    override fun onNewInvitedRoom(session: Session, roomId: String) {
        session.coroutineScope.launch {
            session.roomService().joinRoom(roomId)
        }
    }

    override fun onStatisticsEvent(session: Session, statisticEvent: StatisticEvent) {}

    override fun onSessionStopped(session: Session) {
        session.coroutineScope.coroutineContext.cancelChildren()
    }

    override fun onClearCache(session: Session) {
        session.coroutineScope.coroutineContext.cancelChildren()
    }
}
