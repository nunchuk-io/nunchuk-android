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

package com.nunchuk.android.wallet.components.coin.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.wallet.R

@Composable
fun CoinListTopBarNoneMode(
    enableSelectMode: () -> Unit,
    onShowMoreOptions: () -> Unit,
    title: String,
    isShowMore: Boolean,
) {
    NcTopAppBar(
        title = title,
        textStyle = NunchukTheme.typography.titleLarge,
        isBack = false,
        actions = {
            Text(
                modifier = Modifier.clickable { enableSelectMode() },
                text = stringResource(R.string.nc_select),
                style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
            )
            if (isShowMore) {
                IconButton(onClick = onShowMoreOptions) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more),
                        contentDescription = "More icon"
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
        },
    )
}

@Composable
fun CoinListTopBarSelectMode(
    isSelectAll: Boolean,
    onSelectOrUnselectAll: (isSelect: Boolean) -> Unit = {},
    onSelectDone: () -> Unit
) {
    Surface(tonalElevation = AppBarDefaults.TopAppBarElevation) {
        Row(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .clickable { onSelectOrUnselectAll(isSelectAll.not()) },
                text = if (isSelectAll) stringResource(R.string.nc_unselect_all) else stringResource(
                    R.string.nc_select_all
                ),
                style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
            )
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.nc_select_coins), style = NunchukTheme.typography.titleLarge
            )
            Text(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clickable { onSelectDone() },
                text = stringResource(R.string.nc_text_done),
                style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
            )
        }
    }
}