package com.nunchuk.android.core.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.nunchuk.android.core.R
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

fun Activity.startActivityAppSetting() = startActivity(
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:" + application.packageName)
    )
)

fun Fragment.showLoading() {
    activity?.let(FragmentActivity::showLoading)
}

fun Fragment.hideLoading() {
    activity?.let(FragmentActivity::hideLoading)
}

fun Fragment.showOrHideLoading(loading: Boolean) {
    activity?.showOrHideLoading(loading)
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

fun View.hideKeyboard() = ViewCompat.getWindowInsetsController(this)?.hide(WindowInsetsCompat.Type.ime())

fun Activity.openSelectFileChooser() {
    val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
    startActivityForResult(Intent.createChooser(intent, getString(R.string.nc_text_select_file)), CHOOSE_FILE_REQUEST_CODE)
}

const val CHOOSE_FILE_REQUEST_CODE = 1248