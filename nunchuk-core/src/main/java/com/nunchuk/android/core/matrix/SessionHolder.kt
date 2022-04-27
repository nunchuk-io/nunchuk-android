package com.nunchuk.android.core.matrix

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.asFlow
import com.nunchuk.android.core.util.isAtLeastStarted
import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import timber.log.Timber

object SessionHolder {
    var activeSession: Session? = null

    var currentRoom: Room? = null

    // isOpen state is hidden inside matrix sdk, there is no way to know exactly variable value
    fun storeActiveSession(session: Session) {
        Timber.tag("MainActivityViewModel").d("storeActiveSession of ${session.myUserId}")
        session.apply {
            activeSession = this
            cryptoService().setWarnOnUnknownDevices(false)
            try {
                open()
                if (!hasAlreadySynced()) {
                    startSync(false)
                } else {
                    startSync(ProcessLifecycleOwner.get().isAtLeastStarted())
                }
            } catch (e: Error) {
                CrashlyticsReporter.recordException(e)
            }
        }
    }

    fun clearActiveSession() {
        try {
            activeSession?.close()
        } catch (e: Error) {
            CrashlyticsReporter.recordException(e)
        }
        activeSession = null
        currentRoom = null
    }

    fun hasActiveSession() = activeSession != null

    fun hasActiveRoom() = currentRoom != null

    fun getActiveRoomId() = currentRoom?.roomId!!

    fun getActiveRoomIdSafe() = currentRoom?.roomId.orEmpty()
}

fun Session.roomSummariesFlow() = getRoomSummariesLive(roomSummaryQueryParams {
    memberships = Membership.activeMemberships()
}).asFlow()
