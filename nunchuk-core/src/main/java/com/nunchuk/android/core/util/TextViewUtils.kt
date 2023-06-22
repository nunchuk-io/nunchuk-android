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

import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView

data class ClickAbleText(val content: String, val onClick: (() -> Unit)? = null)

fun TextView.makeTextLink(vararg texts: ClickAbleText) {
    text = texts.joinToString(" ") { it.content }
    val spannableString = SpannableString(text)
    var startIndexOfLink = -1
    texts.forEach { text ->
        if (text.onClick != null) {
            val clickableSpan = object : ClickableSpan() {
                override fun updateDrawState(textPaint: TextPaint) {
                    textPaint.isUnderlineText = true
                }

                override fun onClick(view: View) {
                    text.onClick.invoke()
                }
            }
            startIndexOfLink = this.text.toString().indexOf(text.content, startIndexOfLink + 1)
            spannableString.setSpan(
                clickableSpan,
                startIndexOfLink,
                startIndexOfLink + text.content.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    movementMethod = LinkMovementMethod.getInstance()
    setText(spannableString, TextView.BufferType.SPANNABLE)
}

fun TextView.makeTextLink(fullText: String, vararg texts: ClickAbleText) {
    val spannableString = SpannableString(fullText)
    var startIndexOfLink = -1
    texts.forEach { text ->
        if (text.onClick != null) {
            val clickableSpan = object : ClickableSpan() {
                override fun updateDrawState(textPaint: TextPaint) {
                    textPaint.isUnderlineText = true
                }

                override fun onClick(view: View) {
                    text.onClick.invoke()
                }
            }
            startIndexOfLink = this.text.toString().indexOf(text.content, startIndexOfLink + 1)
            spannableString.setSpan(
                clickableSpan,
                startIndexOfLink,
                startIndexOfLink + text.content.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    movementMethod = LinkMovementMethod.getInstance()
    setText(spannableString, TextView.BufferType.SPANNABLE)
}