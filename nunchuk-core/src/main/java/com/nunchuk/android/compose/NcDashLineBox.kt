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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.dashedBorder
import com.nunchuk.android.core.R

@Composable
fun NcDashLineBox(
    modifier: Modifier = Modifier,
    width: Dp = 2.dp,
    dashWidth: Dp = 8.dp,
    color: Color = colorResource(id = R.color.nc_text_primary),
    showDashedBorder: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
        Box(
            modifier = if (showDashedBorder) {
                modifier.dashedBorder(width, color, RoundedCornerShape(8.dp), dashWidth, dashWidth)
            } else {
                modifier
            }, 
            contentAlignment = Alignment.Center
        ) {
            content()
        }
}

@Preview
@Composable
fun NcDashLineBoxPreview() {
    NcDashLineBox(Modifier.fillMaxWidth().height(height = 100.dp)) {
        Text(textAlign = TextAlign.Center, text = "Demo Text Content")
    }
}
