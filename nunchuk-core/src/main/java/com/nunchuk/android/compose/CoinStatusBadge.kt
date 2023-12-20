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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.core.util.toColor
import com.nunchuk.android.core.util.toName
import com.nunchuk.android.model.UnspentOutput

@Composable
fun CoinStatusBadge(output: UnspentOutput) {
    val name = output.status.toName(LocalContext.current)
    if (name.isNotEmpty()) {
        Text(
            modifier = Modifier
                .padding(start = 4.dp)
                .background(
                    Color(output.status.toColor(LocalContext.current)),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 8.dp),
            text = name,
            style = NunchukTheme.typography.caption.copy(fontSize = 10.sp),
        )
    }
}
