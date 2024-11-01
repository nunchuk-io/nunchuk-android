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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.ClickAbleText

@Composable
fun NcHintMessage(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = NunchukTheme.typography.titleSmall,
    messages: List<ClickAbleText>,
    type: HighlightMessageType = HighlightMessageType.HINT
) {
    val backgroundColor = when(type) {
        HighlightMessageType.WARNING -> colorResource(id = R.color.nc_beeswax_tint)
        HighlightMessageType.HINT -> colorResource(id = R.color.nc_bg_mid_gray)
    }
    val icon =  when(type) {
        HighlightMessageType.HINT -> painterResource(id = R.drawable.ic_info)
        HighlightMessageType.WARNING -> painterResource(id = R.drawable.ic_warning_amber)
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            NcIcon(
                modifier = Modifier.size(36.dp),
                painter = icon,
                contentDescription = "Info icon"
            )
            NcClickableText(
                modifier = Modifier.padding(start = 8.dp),
                messages = messages,
                style = textStyle
            )
        }
    }
}

@Composable
fun NcHintMessage(
    modifier: Modifier = Modifier,
    type: HighlightMessageType = HighlightMessageType.HINT,
    content: @Composable () -> Unit,
) {
    val backgroundColor = when(type) {
        HighlightMessageType.WARNING -> colorResource(id = R.color.nc_beeswax_tint)
        HighlightMessageType.HINT -> colorResource(id = R.color.nc_bg_mid_gray)
    }
    val icon =  when(type) {
        HighlightMessageType.HINT -> painterResource(id = R.drawable.ic_info)
        HighlightMessageType.WARNING -> painterResource(id = R.drawable.ic_warning_amber)
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Crop,
                painter = icon,
                contentDescription = "Info icon"
            )
            content()
        }
    }
}

enum class HighlightMessageType {
    HINT, WARNING
}

@Preview
@Composable
fun NcHintMessageTypeWarningPreview() {
    NunchukTheme {
        NcHintMessage(
            Modifier.padding(16.dp), messages = listOf(
                ClickAbleText("This step requires hardware keys to complete. If you have not received your hardware after a while, please contact us at support@nunchuk.io")
            ),
            type = HighlightMessageType.WARNING
        )
    }
}

@Preview
@Composable
fun NcHintMessageTypeHintPreview() {
    NunchukTheme {
        NcHintMessage(
            Modifier.padding(16.dp), messages = listOf(
                ClickAbleText("This step requires hardware keys to complete. If you have not received your hardware after a while, please contact us at support@nunchuk.io")
            ),
            type = HighlightMessageType.HINT
        )
    }
}
