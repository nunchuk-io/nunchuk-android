package com.nunchuk.android.notifications

import android.content.Intent

data class PushNotificationData(val title: String, val message: String, val intent: Intent)