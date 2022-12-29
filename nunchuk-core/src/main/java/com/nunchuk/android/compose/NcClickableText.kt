package com.nunchuk.android.compose

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.nunchuk.android.core.util.ClickAbleText

@Composable
fun NcClickableText(
    modifier: Modifier,
    messages: List<ClickAbleText>,
    style: TextStyle = NunchukTheme.typography.titleSmall
) {
    val annotatedString = buildAnnotatedString {
        messages.forEachIndexed { index, message ->
            if (message.onClick != null) {
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(message.content)
                }
            } else {
                append(message.content)
            }
            if (index != messages.lastIndex) {
                append(" ")
            }
        }
    }
    ClickableText(
        modifier = modifier,
        text = annotatedString,
        style = style
    ) { offset ->
        var count = offset
        messages.forEach {
            count -= it.content.length.inc()
            if (count <= 0) {
                it.onClick?.invoke()
                return@forEach
            }
        }
    }
}