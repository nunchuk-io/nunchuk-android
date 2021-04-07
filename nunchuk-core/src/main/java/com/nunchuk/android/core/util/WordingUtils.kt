package com.nunchuk.android.core.util

import java.lang.StringBuilder

fun String.shorten(): String {
    if (this.contains(" ")) {
        val words = split(" ")
        val initials = StringBuilder("")
        for (s in words) {
            initials.append(s[0])
        }
        return "$initials"
    }
    return if (length > 2) this.take(2) else this
}