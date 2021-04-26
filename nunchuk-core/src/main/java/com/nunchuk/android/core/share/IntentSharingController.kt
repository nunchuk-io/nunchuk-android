package com.nunchuk.android.core.share

import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import javax.inject.Inject

class IntentSharingController @Inject constructor(val context: Context) {

    private val receiver: Intent = Intent(context, IntentSharingReceiver::class.java)
    private val pendingIntent: PendingIntent = getBroadcast(context, 0, receiver, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)

    fun share(intent: Intent, title: String = "Nunchuk") {
        context.startActivity(Intent.createChooser(intent, title, pendingIntent.intentSender))
    }

}