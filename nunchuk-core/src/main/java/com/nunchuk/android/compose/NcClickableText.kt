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