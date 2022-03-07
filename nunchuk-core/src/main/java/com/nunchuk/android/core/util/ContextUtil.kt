package com.nunchuk.android.core.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat

fun getHtmlText(text: String) = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)

fun Context.getHtmlText(@StringRes resId: Int, arg: String) = getHtmlText(getString(resId, arg))
