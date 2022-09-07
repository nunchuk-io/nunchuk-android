package com.nunchuk.android.core.util

import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes

data class ClickAbleText(@StringRes val content: String, val onClick : (() -> Unit)? = null)

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
                clickableSpan, startIndexOfLink, startIndexOfLink + text.content.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    movementMethod = LinkMovementMethod.getInstance()
    setText(spannableString, TextView.BufferType.SPANNABLE)
}