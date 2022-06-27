package com.nunchuk.android.core.matrix

import org.matrix.android.sdk.api.session.Session

class MatrixEvenBus {

    private var listeners = HashSet<MatrixEventListener>()

    fun subscribe(listener: MatrixEventListener) {
        listeners.add(listener)
    }

    fun publish(session: MatrixEvent) {
        listeners.forEach {
            it(session)
        }
    }

    fun unsubscribe(listener: MatrixEventListener) {
        listeners.remove(listener)
    }

    companion object {
        val instance = InstanceHolder.instance
    }

    private object InstanceHolder {
        var instance = MatrixEvenBus()
    }
}

typealias MatrixEventListener = (MatrixEvent) -> Unit

sealed class MatrixEvent {
    data class SignedInEvent(val session: Session) : MatrixEvent()
    object RoomTransactionCreated : MatrixEvent()
}
