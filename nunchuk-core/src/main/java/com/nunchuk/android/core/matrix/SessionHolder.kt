package com.nunchuk.android.core.matrix

import com.nunchuk.android.model.SendEventExecutor
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.Room

object SessionHolder {
    var currentSession: Session? = null
    var currentRoom: Room? = null
    var sendEventExecutor = object : SendEventExecutor {
        override fun execute(roomId: String, type: String, content: String): String {
            currentRoom?.sendEvent(type, content.toContent())
            return ""
        }
    }

}
