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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun NcCircleImage(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 0.dp,
    iconTintColor: Color = MaterialTheme.colorScheme.primary,
    color: Color = colorResource(id = R.color.nc_whisper_color),
    @DrawableRes resId: Int,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (iconSize > 0.dp) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(id = resId),
                tint = iconTintColor,
                contentDescription = null
            )
        } else {
            Icon(
                painter = painterResource(id = resId),
                tint = iconTintColor,
                contentDescription = null,
            )
        }
    }
}

@Composable
fun NcCircleImage2(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 0.dp,
    color: Color = colorResource(id = R.color.nc_whisper_color),
    @DrawableRes resId: Int,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (iconSize > 0.dp) {
            Image(
                modifier = Modifier.size(iconSize),
                painter = painterResource(id = resId),
                contentDescription = null
            )
        } else {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
fun NcCircleImagePreview() {
    NunchukTheme {
        NcCircleImage(resId = R.drawable.ic_nfc_card)
    }
}