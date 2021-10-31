package com.nunchuk.android.core.bus

class RestartAppEventBus {
    private var callback: (() -> Unit)? = null

    fun subscribe(callback: () -> Unit) {
        this.callback = callback
    }

    fun publish() {
        callback?.invoke()
    }

    fun unsubscribe() {
        callback = null
    }

    companion object {
        private val INSTANCE: RestartAppEventBus = RestartAppEventBus()
        fun instance() = INSTANCE
    }

}