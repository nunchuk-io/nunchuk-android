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

import androidx.compose.material3.Text
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