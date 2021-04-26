package com.nunchuk.android.core.share

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class IntentSharingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        IntentSharingEventBus.instance.publish()
    }

}