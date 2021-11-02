package com.nunchuk.android.core.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nunchuk.android.utils.CrashlyticsReporter

private const val CAMERA_PERMISSION_REQUEST_CODE = 0x1024
private const val READ_STORAGE_PERMISSION_REQUEST_CODE = 0x2048

fun Activity.isPermissionGranted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED

fun Activity.checkReadExternalPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return false
    }
    val isGranted = isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
    if (!isGranted) {
        requestReadExternalPermission()
    }
    return isGranted
}

fun Activity.requestReadExternalPermission() = try {
    ActivityCompat.requestPermissions(
        this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_STORAGE_PERMISSION_REQUEST_CODE
    )
} catch (e: Exception) {
    CrashlyticsReporter.recordException(e)
}

// TODO eliminate duplicated
fun Activity.checkCameraPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return false
    }
    val isGranted = isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
    if (!isGranted) {
        requestCameraPermission()
    }
    return isGranted
}

fun Activity.requestCameraPermission() = try {
    ActivityCompat.requestPermissions(
        this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE
    )
} catch (e: Exception) {
    CrashlyticsReporter.recordException(e)
}
