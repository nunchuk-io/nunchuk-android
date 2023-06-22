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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.R

@Composable
fun SelectCoinCreateTransactionBottomBar(
    amount: Amount = Amount(),
    selectedCoin: Set<UnspentOutput> = emptySet(),
    isExpand: Boolean = false,
    onViewSelectedTransactionCoin: () -> Unit = {},
    onUseCoinClicked: () -> Unit = {},
) {
    val total = selectedCoin.sumOf { it.amount.value }
    Column {
        Row(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .align(alignment = Alignment.CenterHorizontally)
                .clickable(onClick = onViewSelectedTransactionCoin),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nc_selected, Amount(total).getBTCAmount()),
                style = NunchukTheme.typography.titleSmall,
            )
            Icon(
                modifier = Modifier.padding(start = 4.dp),
                painter = painterResource(id = if (isExpand) R.drawable.ic_expand else R.drawable.ic_collapse),
                contentDescription = "Expand Or Collapse",
            )
        }
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterHorizontally),
            text = stringResource(R.string.nc_please_select_at_least, amount.getBTCAmount()),
            style = NunchukTheme.typography.bodySmall,
        )
        NcPrimaryDarkButton(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            onClick = onUseCoinClicked,
            enabled = total >= amount.value
        ) {
            Text(text = stringResource(R.string.nc_use_selected_coins))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectCoinCreateTransactionBottomBarPreview() {
    NunchukTheme {
        Column {
            SelectCoinCreateTransactionBottomBar()
        }
    }
}