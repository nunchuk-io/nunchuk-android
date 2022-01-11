package com.nunchuk.android.notifications

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import com.nunchuk.android.core.persistence.NCSharePreferences
import com.nunchuk.android.utils.CrashlyticsReporter
import javax.inject.Inject

class PushNotificationHelper @Inject constructor(
    private val context: Context,
    private val preferences: NCSharePreferences
) {

    fun getFcmToken(): String = preferences.fcmToken.orEmpty()

    fun storeFcmToken(token: String?) {
        preferences.fcmToken = token
    }

    fun retrieveFcmToken(isNotificationEnabled: Boolean, onTokenRetrieved: (String) -> Unit = {}, onServiceNotAvailable: () -> Unit) {
        if (checkPlayServices()) {
            try {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        storeFcmToken(token)
                        if (isNotificationEnabled) {
                            onTokenRetrieved(token)
                        }
                    }
                    .addOnFailureListener {
                        CrashlyticsReporter.recordException(it)
                    }
            } catch (e: Throwable) {
                CrashlyticsReporter.recordException(e)
            }
        } else {
            onServiceNotAvailable()
        }
    }

    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

}
