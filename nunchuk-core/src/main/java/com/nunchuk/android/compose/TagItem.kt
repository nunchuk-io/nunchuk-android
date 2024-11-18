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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.core.util.hexToColor

@Composable
fun TagItem(
    modifier: Modifier = Modifier,
    name: String = "",
    color: String = "",
    numCoins: Int = 0,
    checked: Boolean = false,
    tagFlow: Int = TagFlow.NONE,
    onTagClick: () -> Unit = {},
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable {
                if (tagFlow == TagFlow.VIEW) {
                    onTagClick()
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp, 48.dp)
                .clip(CircleShape)
                .background(color = color.hexToColor())
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = name,
                style = NunchukTheme.typography.body
            )
            Text(
                text = stringResource(id = R.string.nc_num_coins_data, numCoins.toString()),
                modifier = Modifier.padding(top = 4.dp),
                style = NunchukTheme.typography.bodySmall
            )
        }
        if (tagFlow == TagFlow.VIEW) {
            NcIcon(
                painter = painterResource(id = R.drawable.ic_arrow),
                contentDescription = ""
            )
        } else {
            NcCheckBox(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TagItemPreview() {
    NunchukTheme {
        Column(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) {
            TagItem(
                name = "Tag 1",
                color = "#FF0000",
                numCoins = 10,
                checked = false
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TagItemCheckedPreview() {
    NunchukTheme {
        Column(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) {
            TagItem(
                name = "Tag 1",
                color = "#FF0000",
                numCoins = 10,
                checked = true
            )
        }
    }
}