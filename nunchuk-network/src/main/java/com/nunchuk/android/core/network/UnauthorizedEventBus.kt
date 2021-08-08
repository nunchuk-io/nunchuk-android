package com.nunchuk.android.core.network

class UnauthorizedEventBus {
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
        private val INSTANCE: UnauthorizedEventBus = UnauthorizedEventBus()
        fun instance() = INSTANCE
    }

}