package com.nunchuk.android.utils

import android.app.PendingIntent
import android.os.Build

object PendingIntentUtils {
    fun getFlagCompat() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_MUTABLE
    } else {
        0
    }
}