package com.nunchuk.android.core.util

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
fun Activity.isPermissionGranted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED

fun Activity.startActivityAppSetting() = startActivity(
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:" + application.packageName)
    )
)

