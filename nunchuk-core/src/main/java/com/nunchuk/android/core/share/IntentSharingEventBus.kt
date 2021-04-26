package com.nunchuk.android.core.share

class IntentSharingEventBus {

    private var listener: IntentSharingListener? = null

    fun subscribe(listener: IntentSharingListener) {
        this.listener = listener
    }

    fun unsubscribe() {
        listener = null
    }

    fun publish() {
        listener?.onCompleted()
    }

    companion object {
        val instance = InstanceHolder.instance
    }

    private object InstanceHolder {
        var instance = IntentSharingEventBus()
    }
}