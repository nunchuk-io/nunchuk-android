package com.nunchuk.android.core.util

class AppEvenBus {

    private var listeners = HashSet<AppEventListener>()

    fun subscribe(listener: AppEventListener) {
        listeners.add(listener)
    }

    fun publish(session: AppEvent) {
        listeners.forEach {
            it(session)
        }
    }

    fun unsubscribe(listener: AppEventListener) {
        listeners.remove(listener)
    }

    companion object {
        val instance = InstanceHolder.instance
    }

    private object InstanceHolder {
        var instance = AppEvenBus()
    }
}

typealias AppEventListener = (AppEvent) -> Unit

sealed class AppEvent {
    object AppResumedEvent : AppEvent()
}
