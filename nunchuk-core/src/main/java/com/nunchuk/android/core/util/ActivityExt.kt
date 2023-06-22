/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import java.io.File

fun Activity.showToast(message: String) = NCToastMessage(this).show(message)

fun Activity.showLoading() {
    (this as BaseActivity<*>).showLoading()
}

fun Activity.hideLoading() {
    (this as BaseActivity<*>).hideLoading()
}

fun Activity.showOrHideLoading(
    loading: Boolean, title: String = getString(R.string.nc_please_wait), message: String? = null
) {
    (this as BaseActivity<*>).showOrHideLoading(loading, title, message)
}

fun Fragment.showLoading() {
    activity?.let(FragmentActivity::showLoading)
}

fun Fragment.hideLoading() {
    activity?.let(FragmentActivity::hideLoading)
}

fun Fragment.showOrHideNfcLoading(loading: Boolean, isColdCard: Boolean = false) {
    if (isColdCard) {
        showOrHideLoading(
            loading,
            title = getString(R.string.nc_data_transfer_in_progress),
            message = getString(R.string.nc_keep_hold_coldcard_until_finish),
        )
    } else {
        showOrHideLoading(loading, message = getString(R.string.nc_keep_holding_nfc))
    }
}

fun Activity.showOrHideNfcLoading(loading: Boolean, isColdCard: Boolean = false) {
    if (isColdCard) {
        showOrHideLoading(
            loading,
            title = getString(R.string.nc_data_transfer_in_progress),
            message = getString(R.string.nc_keep_hold_coldcard_until_finish),
        )
    } else {
        showOrHideLoading(loading, message = getString(R.string.nc_keep_holding_nfc))
    }
}

fun Fragment.showOrHideLoading(
    loading: Boolean, title: String = getString(R.string.nc_please_wait), message: String? = null
) {
    activity?.showOrHideLoading(loading, title, message)
}

fun View.hideKeyboard() =
    ViewCompat.getWindowInsetsController(this)?.hide(WindowInsetsCompat.Type.ime())

fun Activity.openSelectFileChooser(requestCode: Int = CHOOSE_FILE_REQUEST_CODE) {
    val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
    startActivityForResult(
        Intent.createChooser(intent, getString(R.string.nc_text_select_file)), requestCode
    )
}

fun Fragment.openSelectFileChooser(requestCode: Int = CHOOSE_FILE_REQUEST_CODE) {
    val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
    startActivityForResult(
        Intent.createChooser(intent, getString(R.string.nc_text_select_file)), requestCode
    )
}

fun Activity.openExternalLink(url: String) {
    if (url.isNotEmpty()) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(webIntent)
        } catch (e: ActivityNotFoundException) {
            CrashlyticsReporter.recordException(e)
            NCToastMessage(this).showWarning(getString(R.string.nc_transaction_no_app_to_open_link))
        }
    }
}

fun Context.openExternalLink(url: String) {
    if (url.isNotEmpty()) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(webIntent)
        } catch (e: ActivityNotFoundException) {
            CrashlyticsReporter.recordException(e)
        }
    }
}

fun Activity.sendEmail(email: String, subject: String = "", text: String = ""): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("mailto:")
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
    if (subject.isNotEmpty()) intent.putExtra(Intent.EXTRA_SUBJECT, subject)
    if (text.isNotEmpty()) intent.putExtra(Intent.EXTRA_TEXT, text)
    val result = runCatching {
        startActivity(intent)
    }
    return result.isSuccess
}

fun getFileFromUri(contentResolver: ContentResolver, uri: Uri, directory: File): File? = try {
    val file = File.createTempFile("NCsuffix", ".prefixNC", directory)
    file.outputStream().use {
        contentResolver.openInputStream(uri)?.copyTo(it)
    }
    file
} catch (t: Throwable) {
    CrashlyticsReporter.recordException(t)
    null
}

fun <T> AppCompatActivity.flowObserver(
    flow: Flow<T>,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    collector: FlowCollector<T>
) {
    lifecycleScope.launch {
        flow.flowWithLifecycle(lifecycle, state).collect(collector)
    }
}

const val CHOOSE_FILE_REQUEST_CODE = 1248
