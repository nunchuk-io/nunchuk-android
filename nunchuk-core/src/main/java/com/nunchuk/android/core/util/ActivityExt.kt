package com.nunchuk.android.core.util

import android.app.Activity
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.nunchuk.android.core.base.BaseActivity

fun Activity.showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Activity.showLoading() {
    (this as BaseActivity).showLoading()
}

fun Activity.hideLoading() {
    (this as BaseActivity).hideLoading()
}

fun Fragment.showLoading() {
    activity?.let(FragmentActivity::showLoading)
}

fun Fragment.hideLoading() {
    activity?.let(FragmentActivity::hideLoading)
}