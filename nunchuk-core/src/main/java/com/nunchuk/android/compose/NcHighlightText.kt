package com.nunchuk.android.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Composable
fun NcHighlightText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = NunchukTheme.typography.body
) {
    var start = 0
    val openBold = "[B]"
    val closedBold = "[/B]"
    val annotatedString = buildAnnotatedString {
        while (start < text.length) {
            val startBold = text.indexOf(openBold, start)
            val endBold = text.indexOf(closedBold, start)
            start = if (endBold == -1) {
                append(text.substring(start, text.length))
                text.length
            } else {
                if (start < startBold) {
                    append(text.substring(start, startBold))
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(startBold + openBold.length, endBold))
                }
                endBold + closedBold.length
            }
        }
    }
    Text(
        modifier = modifier, text = annotatedString, style = style
    )
}