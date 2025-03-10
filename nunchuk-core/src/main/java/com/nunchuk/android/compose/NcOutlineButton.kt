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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun NcOutlineButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    borderColor: Color = colorResource(id = R.color.nc_fill_primary),
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val color = if (enabled) borderColor else MaterialTheme.colorScheme.whisper
    OutlinedButton(
        modifier = modifier.height(height),
        enabled = enabled,
        onClick = onClick,
        border = BorderStroke(2.dp, color),
        shape = RoundedCornerShape(44.dp),
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides NunchukTheme.typography.title.copy(
                    color = colorResource(id = R.color.nc_fill_primary)
                )
            ) {
                content()
            }
        },
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
    )
}

@PreviewLightDark
@Composable
private fun NcOutlineButtonPreview() {
    NcOutlineButton(
        onClick = {},
        content = {
            Text("Button")
        }
    )
}