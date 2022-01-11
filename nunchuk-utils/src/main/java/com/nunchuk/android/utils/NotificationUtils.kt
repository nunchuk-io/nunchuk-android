package com.nunchuk.android.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object NotificationUtils {

    private const val APP_NOTIFICATION_SETTINGS = "android.settings.APP_NOTIFICATION_SETTINGS"
    private const val APP_PACKAGE = "app_package"
    private const val APP_UID = "app_uid"

    fun areNotificationsEnabled(context: Context) = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun openNotificationSettings(context: Context) {
        val intent = Intent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            intent.action = APP_NOTIFICATION_SETTINGS
        }
        intent.putExtra(APP_PACKAGE, context.packageName)
        intent.putExtra(APP_UID, context.applicationInfo.uid)
        context.startActivity(intent)
    }

}