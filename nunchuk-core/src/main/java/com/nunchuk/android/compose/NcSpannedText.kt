package com.nunchuk.android.compose

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview

data class SpanIndicator(val openTag: String, val closeTag: String) {
    constructor(character: Char) : this("[${character}]", "[/${character}]")
}

@Composable
fun NcSpannedText(
    modifier: Modifier = Modifier,
    text: String,
    baseStyle: TextStyle,
    textAlign: TextAlign? = null,
    styles: Map<SpanIndicator, SpanStyle> = emptyMap(),
) {
    Text(
        modifier = modifier,
        text = spannedText(text, styles),
        style = baseStyle,
        textAlign = textAlign
    )
}

@Composable
fun NcSpannedClickableText(
    modifier: Modifier = Modifier,
    text: String,
    baseStyle: TextStyle,
    styles: Map<SpanIndicator, SpanStyle> = emptyMap(),
    onClick: (tag: String) -> Unit
) {
    val annotatedString = spannedText(text, styles)
    ClickableText(
        modifier = modifier,
        text = spannedText(text, styles),
        style = baseStyle,
        onClick = { pos ->
            annotatedString.getStringAnnotations(ANNOTATION_TAG, pos, pos.inc())
                .firstOrNull()?.let {
                    onClick(it.item)
                }
        }
    )
}

private fun spannedText(value: String, styles: Map<SpanIndicator, SpanStyle>) =
    spannedTextWithAnnotation(value, styles)

@OptIn(ExperimentalTextApi::class)
private fun spannedTextWithAnnotation(
    value: String,
    styles: Map<SpanIndicator, SpanStyle>,
) = buildAnnotatedString {
    var temp = value
    styles.toSortedMap(compareBy { value.indexOf(it.openTag) }).forEach { item ->
        val start = temp.indexOf(string = item.key.openTag)
        val end = temp.indexOf(string = item.key.closeTag, startIndex = start)
        if (start > 0) {
            append(temp.substring(0, start))
        }
        if (start >= 0 && (start + item.key.openTag.length < end)) {
            withAnnotation(ANNOTATION_TAG, item.key.openTag) {
                withStyle(item.value) {
                    append(temp.substring(start + item.key.openTag.length, end))
                }
            }
            val index = end + item.key.closeTag.length
            if (index < temp.length + 1) {
                temp = temp.substring(index)
            }
        }
    }
    if (temp.isNotEmpty()) {
        append(temp)
    }
}

private const val ANNOTATION_TAG = "tag"

@Preview(showBackground = true)
@Composable
fun NcSpannedTextPreviewOne() {
    NcSpannedText(
        text = "<b>Google Pay</b> (subscription)",
        baseStyle = MaterialTheme.typography.titleSmall,
        styles = hashMapOf(
            SpanIndicator('b') to SpanStyle(color = Color.Red)
        )
    )
}