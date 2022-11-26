package com.nunchuk.android.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun NcHighlightText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = NunchukTheme.typography.body
) {
    var start = 0
    val annotatedString = buildAnnotatedString {
        while (start < text.length) {
            val startBold = text.indexOf("<b>")
            val endBold = text.indexOf("</b>")
            if (endBold == -1) {
                append(text.substring(start, text.length))
                start = text.length
            } else if (start < startBold) {
                append(text.substring(start, startBold))
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(startBold + 3, endBold + 4))
                }
                start = endBold + 4
            }
        }
    }
    Text(
        modifier = modifier, text = annotatedString, style = style
    )
}

@Preview
@Composable
private fun NcHighlightTextPreview() {
    NcHighlightText(text = "<b>The inheritance</b> key is designed to be used exclusively for the purpose of inheritance. The TAPSIGNER designated as the inheritance key should be locked and put away in a safe place, until it is needed.")
}