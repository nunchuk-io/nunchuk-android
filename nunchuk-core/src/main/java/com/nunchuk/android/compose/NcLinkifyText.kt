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

package com.nunchuk.android.compose

import android.util.Patterns
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.nunchuk.android.core.util.openExternalLink
import java.util.regex.Pattern

@Composable
fun NcLinkifyText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onClick : () -> Unit = {},
) {
    val context = LocalContext.current
    val linksList = extractUrls(text)
    val annotatedString = buildAnnotatedString {
        append(text)
        linksList.forEach {
            addStyle(
                style = SpanStyle(
                    textDecoration = TextDecoration.Underline
                ),
                start = it.start,
                end = it.end
            )
            addStringAnnotation(
                tag = "URL",
                annotation = it.url,
                start = it.start,
                end = it.end
            )
        }
    }
    ClickableText(
        modifier = modifier,
        text = annotatedString,
        style = style,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout
    ) { offset ->
        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
            .firstOrNull()?.let {
                val note = it.item
                runCatching {
                    val matcher = Patterns.WEB_URL.matcher(note)
                    if (matcher.find()) {
                        val link = note.substring(matcher.start(1), matcher.end())
                        context.openExternalLink(link)
                    }
                }
        } ?: onClick()
    }
}

@Preview(showBackground = true)
@Composable
fun NcLinkifyTextPreview() {
    NunchukTheme {
        NcLinkifyText(
            text = "Welcome to https://google.com.vn",
            style = NunchukTheme.typography.body
        )
    }
}

private val urlPattern: Pattern = Patterns.WEB_URL

private fun extractUrls(text: String): List<LinkInfos> {
    val matcher = urlPattern.matcher(text)
    var matchStart: Int
    var matchEnd: Int
    val links = arrayListOf<LinkInfos>()

    while (matcher.find()) {
        matchStart = matcher.start(1)
        matchEnd = matcher.end()

        val url = text.substring(matchStart, matchEnd)
        links.add(LinkInfos(url, matchStart, matchEnd))
    }
    return links
}

private data class LinkInfos(
    val url: String,
    val start: Int,
    val end: Int
)