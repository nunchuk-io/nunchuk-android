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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.utils.CoinTagColorUtil

@Composable
fun CoinTagView(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = NunchukTheme.typography.bodySmall,
    circleSize: Dp = 16.dp,
    tag: CoinTag,
    clickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier
            .background(
                color = MaterialTheme.colorScheme.greyLight, shape = RoundedCornerShape(24.dp)
            ).let {
                if (clickable) {
                    it.clickable(onClick = onClick)
                } else it
            }
            .border(1.dp, color = MaterialTheme.colorScheme.strokePrimary, shape = RoundedCornerShape(24.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(Color(tag.color.ifBlank { CoinTagColorUtil.hexColors.first() }.toColorInt()))
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = if (tag.name.length < 20) tag.name else "${tag.name.take(20)}...",
            style = textStyle,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CoinTagViewPreview() {
    NunchukTheme {
        CoinTagView(
            tag = CoinTag(
                id = 1, color = "blue", name = "Kidding"
            )
        )
    }
}