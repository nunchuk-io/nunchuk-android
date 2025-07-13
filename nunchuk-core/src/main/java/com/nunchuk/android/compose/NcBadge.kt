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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.core.R

@Composable
fun NcBadgePrimary(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true
) {
    Text(
        modifier = modifier
            .background(
                color = if (enabled) {
                    MaterialTheme.colorScheme.controlFillPrimary
                } else {
                    colorResource(id = R.color.nc_control_state_activated_40)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = text,
        style = NunchukTheme.typography.caption.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            color = if (enabled) {
                MaterialTheme.colorScheme.controlTextPrimary
            } else {
                colorResource(id = R.color.nc_control_text_primary)
            }
        )
    )
}

@Composable
fun NcBadgeOutline(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.strokePrimary,
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = text,
        style = NunchukTheme.typography.caption.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.textPrimary
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun NcBadgePreview() {
    NunchukTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            NcBadgePrimary(text = "Key path disabled", enabled = false)
            NcBadgePrimary(text = "Script path", enabled = true)
            NcBadgeOutline(text = "Tapscripts")
        }
    }
} 