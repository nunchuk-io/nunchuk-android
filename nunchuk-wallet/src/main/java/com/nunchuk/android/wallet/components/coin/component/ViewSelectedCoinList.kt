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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.coin.MODE_SELECT
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.coin.PreviewCoinCard
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.R

@Composable
fun ViewSelectedCoinList(
    modifier: Modifier = Modifier,
    allTags: Map<Int, CoinTag>,
    selectedCoin: Set<UnspentOutput>,
    coins: List<UnspentOutput>,
    onSelectCoin: (output: UnspentOutput, isSelected: Boolean) -> Unit = { _, _ -> },
    onSelectOrUnselectAll: (isSelect: Boolean, coins: List<UnspentOutput>) -> Unit = {_,_ ->},
) {
    val isSelectAll = selectedCoin.size == coins.size
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.7f))
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Column(
            modifier = Modifier.weight(1f).background(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(vertical = 20.dp, horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(R.string.nc_selected_coins),
                    style = NunchukTheme.typography.titleLarge
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterEnd).clickable {
                        onSelectOrUnselectAll(isSelectAll.not(), coins)
                    },
                    text = if (isSelectAll) stringResource(R.string.nc_unselect_all)
                    else stringResource(R.string.nc_select_all),
                    style = NunchukTheme.typography.titleLarge
                )
            }
            LazyColumn {
                items(coins) { coin ->
                    PreviewCoinCard(
                        output = coin,
                        tags = allTags,
                        mode = MODE_SELECT,
                        isSelected = selectedCoin.contains(coin),
                        onSelectCoin = onSelectCoin
                    )
                }
            }
        }
    }
}