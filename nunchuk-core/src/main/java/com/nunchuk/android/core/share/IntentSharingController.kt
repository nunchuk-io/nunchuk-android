package com.nunchuk.android.core.share

import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.io.File

class IntentSharingController private constructor(val activityContext: Activity) {

    private val receiver: Intent = Intent(activityContext, IntentSharingReceiver::class.java)
    private val pendingIntent: PendingIntent = getBroadcast(activityContext, 0, receiver, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)

    fun share(intent: Intent, title: String = "Nunchuk") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            activityContext.startActivity(Intent.createChooser(intent, title, pendingIntent.intentSender))
        }
    }

    fun shareFile(filePath: String) {
        share(Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(filePath)))
            type = "*/*"
        })
    }

    fun shareText(text: String) {
        share(Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        })
    }

    companion object {
        fun from(activityContext: Activity) = IntentSharingController(activityContext)
    }

}