/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

import java.util.*

private const val DELIMITERS = " "

fun String.shorten(): String {
    if (isEmpty()) return this
    if (this.contains(DELIMITERS)) {
        val words = split(DELIMITERS)
        val initials = words.filter(String::isNotEmpty).map { it[0] }
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