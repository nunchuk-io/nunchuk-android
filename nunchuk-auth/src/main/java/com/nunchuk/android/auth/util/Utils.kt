package com.nunchuk.android.auth.util

import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.widget.TextView

internal fun String?.orUnknownError() = this ?: "Unknown Error"

fun TextView.setUnderlineText(text: String) {
    SpannableString(text).apply {
        setSpan(UnderlineSpan(), 0, length, 0)
        setText(this)
    }
}

