package com.nunchuk.android.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle

const val DEFAULT_MINIMUM_TEXT_LINE = 3

@Composable
fun NcExpandableTextInline(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    style: TextStyle = NunchukTheme.typography.body,
    text: String,
    collapsedMaxLine: Int = DEFAULT_MINIMUM_TEXT_LINE,
    showMoreText: String = "... More",
    showMoreStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.textPrimary),
    showLessText: String = "",
    showLessStyle: SpanStyle = showMoreStyle,
    textAlign: TextAlign? = null,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var clickable by remember { mutableStateOf(false) }
    var lastCharIndex by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier
        .clickable(clickable) {
            isExpanded = !isExpanded
        }
        .then(modifier)
    ) {
        Text(
            modifier = textModifier
                .fillMaxWidth()
                .animateContentSize(),
            text = buildAnnotatedString {
                if (clickable) {
                    if (isExpanded) {
                        append(text)
                        withStyle(style = showLessStyle) { append(showLessText) }
                    } else {
                        kotlin.runCatching {
                            val adjustText = text.substring(startIndex = 0, endIndex = lastCharIndex)
                                .dropLast(showMoreText.length)
                                .dropLastWhile { Character.isWhitespace(it) || it == '.' }
                            append(adjustText)
                            withStyle(style = showMoreStyle) { append(showMoreText) }
                        }.onFailure {
                            append(text)
                        }

                    }
                } else {
                    append(text)
                }
            },
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLine,
            onTextLayout = { textLayoutResult ->
                if (!isExpanded && textLayoutResult.hasVisualOverflow) {
                    clickable = true
                    lastCharIndex = textLayoutResult.getLineEnd(collapsedMaxLine - 1)
                }
            },
            style = style,
            textAlign = textAlign,
        )
    }
}