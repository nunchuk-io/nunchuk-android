package com.nunchuk.android.core.matrix

import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room

object SessionHolder {
    var currentSession: Session? = null
    var currentRoom: Room? = null
}
