package com.nunchuk.android.core.matrix

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.asFlow
import com.nunchuk.android.core.util.isAtLeastStarted
import com.nunchuk.android.log.fileLog
import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionHolder @Inject constructor(
    private val sessionListener: SessionListener
) {
    private var activeSessionReference: AtomicReference<Session?> = AtomicReference()

    var currentRoom: Room? = null

    // isOpen state is hidden inside matrix sdk, there is no way to know exactly variable value
    fun storeActiveSession(session: Session) {
        fileLog(message = "storeActiveSession of ${session.myUserId}")
        getSafeActiveSession()?.apply {
            removeListener(sessionListener)
            close()
        }
        session.apply {
            activeSessionReference.set(this)
            addListener(sessionListener)
            cryptoService().setWarnOnUnknownDevices(false)
            try {
                open()
                if (!syncService().hasAlreadySynced()) {
                    syncService().startSync(true)
                } else {
                    syncService().startSync(ProcessLifecycleOwner.get().isAtLeastStarted())
                }
                pushersService().refreshPushers()
            } catch (e: Exception) {
                CrashlyticsReporter.recordException(e)
            }
        }
    }

    fun clearActiveSession() {
        try {
            getSafeActiveSession()?.apply {
                removeListener(sessionListener)
                close()
            }
        } catch (e: Error) {
            CrashlyticsReporter.recordException(e)
        }
        activeSessionReference.set(null)
        currentRoom = null
    }

    fun getSafeActiveSession(): Session? {
        return activeSessionReference.get()
    }

    fun hasActiveSession() = activeSessionReference.get() != null

    fun hasActiveRoom() = currentRoom != null

    fun getActiveRoomId() = currentRoom?.roomId!!

    fun getActiveRoomIdSafe() = currentRoom?.roomId.orEmpty()
}

fun Session.roomSummariesFlow() = roomService().getRoomSummariesLive(roomSummaryQueryParams {
    memberships = Membership.activeMemberships()
}).asFlow()
