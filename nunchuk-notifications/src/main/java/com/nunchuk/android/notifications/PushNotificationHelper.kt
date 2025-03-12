/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import com.nunchuk.android.utils.CrashlyticsReporter
import javax.inject.Inject

private const val CHANNEL_ID = "io.nunchuk.android.channelId"
private const val CHANNEL_NAME = "Nunchuk Notification Center"

class PushNotificationHelper @Inject constructor(
    private val context: Context,
) {

    fun retrieveFcmToken(
        onTokenRetrieved: (String) -> Unit = {},
    ) {
        try {
            if (checkPlayServices()) {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        onTokenRetrieved(token)
                    }
                    .addOnFailureListener(CrashlyticsReporter::recordException)
            }
        } catch (e: Throwable) {
            CrashlyticsReporter.recordException(e)
        }
    }

    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

}

@SuppressLint("InlinedApi")
fun Context.showNotification(data: PushNotificationData, mainIntent: Intent? = null) {
    val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setSmallIcon(R.drawable.ic_logo_notification)
        .setContentTitle(data.title)
        .setContentText(data.message)
        .setAutoCancel(true)


    val requestCode = (System.currentTimeMillis() % 10000).toInt()
    val resultPendingIntent = if (mainIntent != null) {
        // Configure the intent stack: MainActivity -> Target Activity
        val targetIntent = data.intent

        // Build the task stack to ensure MainActivity is the root
        val taskBuilder = TaskStackBuilder.create(this)
        taskBuilder.addNextIntentWithParentStack(mainIntent)
        taskBuilder.addNextIntent(targetIntent)
        taskBuilder.getPendingIntent(
            requestCode,
            PendingIntent.FLAG_IMMUTABLE
        )
    } else {
        PendingIntent.getActivity(this, requestCode, data.intent, PendingIntent.FLAG_IMMUTABLE)
    }
    builder.setContentIntent(resultPendingIntent)

    val notificationManager = NotificationManagerCompat.from(applicationContext)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        builder.priority = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    if (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        notificationManager.notify((data.id % Int.MAX_VALUE.toLong()).toInt(), builder.build())
    }
}