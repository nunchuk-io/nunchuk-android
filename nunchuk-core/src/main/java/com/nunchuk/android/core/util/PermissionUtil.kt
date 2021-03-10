package com.nunchuk.android.core.util

import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat

fun Activity.isPermissionGranted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED