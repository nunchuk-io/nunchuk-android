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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun NcPrimaryDarkButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    isAutoExpandHeight: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor =
        if (isPressed) colorResource(id = R.color.nc_button_press_state_color) else colorResource(id = R.color.nc_control_fill_primary)
    val contentColor = if (isPressed) colorResource(id = R.color.nc_control_text_secondary) else colorResource(id = R.color.nc_control_text_primary)
    Button(
        enabled = enabled,
        modifier = if (isAutoExpandHeight) modifier.wrapContentHeight() else modifier.height(height),
        onClick = onClick,
        interactionSource = interactionSource,
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides NunchukTheme.typography.title.copy(color = Color.Unspecified),
            ) {
                content()
            }
        },
        shape = RoundedCornerShape(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = colorResource(id = R.color.nc_control_fill_tertiary),
            disabledContentColor = colorResource(R.color.nc_control_text_tertiary)
        )
    )
}

@PreviewLightDark
@Composable
private fun NcPrimaryButtonPreview() {
    NunchukTheme {
        NcPrimaryDarkButton(
            onClick = {},
            content = {
                Text(text = "Primary Button")
            }
        )
    }
}