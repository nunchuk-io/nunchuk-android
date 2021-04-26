package com.nunchuk.android.core.share

interface IntentSharingListener {
    fun onCompleted()
}

class IntentSharingListenerWrapper(val func: () -> Unit) : IntentSharingListener {

    override fun onCompleted() {
        func()
    }

}