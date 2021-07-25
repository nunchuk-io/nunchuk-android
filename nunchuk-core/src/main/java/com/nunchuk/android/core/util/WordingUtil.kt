package com.nunchuk.android.core.util

import java.util.*

private const val DELIMITERS = " "

fun String.shorten(): String {
    if (this.contains(DELIMITERS)) {
        val words = split(DELIMITERS)
        val initials = StringBuilder("")
        for (s in words) {
            initials.append(s[0])
        }
        return "${initials.first()}${initials.last()}".upperCase()
    }
    return "${first()}".upperCase()
}

fun String.upperCase() = toUpperCase(Locale.getDefault())

fun String.lastWord(): String = if (this.contains(DELIMITERS)) {
    this.split(DELIMITERS).last()
} else {
    this
}

fun String.replaceLastWord(word: String) = if (this.contains(DELIMITERS)) {
    "${substring(0, lastIndexOf(DELIMITERS))}$DELIMITERS$word"
} else {
    word
}

fun String.countWords() = when {
    isEmpty() -> 0
    !contains(DELIMITERS) -> 1
    else -> split(DELIMITERS).size
}