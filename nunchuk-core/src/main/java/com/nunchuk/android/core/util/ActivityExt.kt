package com.nunchuk.android.core.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.nunchuk.android.core.base.BaseActivity

fun Activity.showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Activity.showLoading() {
    (this as BaseActivity<*>).showLoading()
}

fun Activity.hideLoading() {
    (this as BaseActivity<*>).hideLoading()
}

fun Activity.showOrHideLoading(loading: Boolean) {
    (this as BaseActivity<*>).showOrHideLoading(loading)
}

fun Fragment.showLoading() {
    activity?.let(FragmentActivity::showLoading)
}

fun Fragment.hideLoading() {
    activity?.let(FragmentActivity::hideLoading)
}

fun Fragment.showOrHideLoading(loading: Boolean) {
    activity?.showOrHideLoading(loading)
}

private const val READ_STORAGE_PERMISSION_REQUEST_CODE = 0x2048

fun Activity.checkReadExternalPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return false
    }
    val result: Int = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    val isGranted = result == PackageManager.PERMISSION_GRANTED
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
    e.printStackTrace()
}

fun Fragment.pickPhotoWithResult(requestCode: Int) {
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    intent.type = "image/*"
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
    intent.action = Intent.ACTION_GET_CONTENT
    startActivityForResult(
        intent, requestCode
    )
}

fun Fragment.takePhotoWithResult(requestCode: Int) {
    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    startActivityForResult(takePictureIntent, requestCode)
}