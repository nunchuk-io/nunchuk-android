package com.nunchuk.android.auth.util

import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.widget.TextView

fun TextView.setUnderlineText(text: String) {
    SpannableString(text).apply {
        setSpan(UnderlineSpan(), 0, length, 0)
        setText(this)
    }
}

