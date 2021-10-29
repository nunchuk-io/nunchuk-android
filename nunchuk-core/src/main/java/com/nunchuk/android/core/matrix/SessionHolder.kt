package com.nunchuk.android.core.matrix

import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room
import timber.log.Timber

object SessionHolder {
    var activeSession: Session? = null

    var currentRoom: Room? = null

    // isOpen state is hidden inside matrix sdk, there is no way to know exactly variable value
    fun storeActiveSession(session: Session) {
        session.apply {
            try {
                open()
                if (!hasAlreadySynced()) {
                    startSync(false)
                }
            } catch (e: Error) {
                CrashlyticsReporter.recordException(e)
            }
            activeSession = this
        }
    }

    suspend fun clearActiveSession() {
        try {
            activeSession?.signOut(true)
        } catch (e: Error) {
            CrashlyticsReporter.recordException(e)
        }
        activeSession = null
        currentRoom = null
    }

    fun hasActiveSession(): Boolean {
        Timber.d("sessionParams::${activeSession?.sessionParams}")
        Timber.d("devicesInfo::${activeSession?.cryptoService()?.getMyDevicesInfo()}")
        return activeSession != null
    }

    fun hasActiveRoom() = currentRoom != null

    fun getActiveRoomId() = currentRoom?.roomId!!
}
