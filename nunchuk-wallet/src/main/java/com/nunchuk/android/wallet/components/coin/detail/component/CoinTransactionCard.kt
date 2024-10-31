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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.TransactionNoteView
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.core.util.formatAddress
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.model.Transaction

@Composable
fun CoinTransactionCard(
    transaction: Transaction,
    onViewTransactionDetail: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .padding(
                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp
            )
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp, color = MaterialTheme.colorScheme.strokePrimary, shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = true, onClick = onViewTransactionDetail)
            .padding(16.dp)
    ) {
        Text(
            text = transaction.formatAddress(LocalContext.current),
            style = NunchukTheme.typography.bodySmall,
        )
        Text(
            text = transaction.totalAmount.getBTCAmount(),
            style = NunchukTheme.typography.body,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (transaction.memo.isNotEmpty()) {
            TransactionNoteView(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.strokePrimary, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                note = transaction.memo
            )
        }
    }
}

@Preview
@Composable
fun CoinTransactionCardPreview() {
    NunchukTheme {
        CoinTransactionCard(transaction = Transaction(memo = "Hello My name is Hai"))
    }
}