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

package com.nunchuk.android.wallet.components.coin.detail.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.R

@Composable
fun LockCoinRow(
    output: UnspentOutput,
    onLockCoin: (isLocked: Boolean) -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp),
            painter = painterResource(id = R.drawable.ic_lock),
            contentDescription = "Lock icon"
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(R.string.nc_lock_this_coin),
            style = NunchukTheme.typography.title
        )
        Spacer(modifier = Modifier.weight(1f))
        NcSwitch(checked = output.isLocked, onCheckedChange = onLockCoin)
    }
}