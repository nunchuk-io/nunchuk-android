package com.nunchuk.android.notifications

import android.content.Intent

interface PushNotificationIntentProvider {
    fun getRoomDetailsIntent(roomId: String): Intent
    fun getMainIntent(): Intent
}