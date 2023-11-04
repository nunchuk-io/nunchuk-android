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

package com.nunchuk.android.transaction.components.send.fee

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.MODE_VIEW_ONLY
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.PreviewCoinCard
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput

@Composable
fun TransactionCoinSelection(
    modifier: Modifier = Modifier, inputs: List<UnspentOutput>, allTags: Map<Int, CoinTag>
) {
    NunchukTheme {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = "Current selection",
                style = NunchukTheme.typography.titleSmall
            )

            inputs.forEach {
                PreviewCoinCard(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .background(color = Color.White, shape = RoundedCornerShape(12.dp)),
                    output = it,
                    mode = MODE_VIEW_ONLY,
                    tags = allTags
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionCoinSelectionPreview() {
    NunchukTheme {
        TransactionCoinSelection(inputs = emptyList(), allTags = emptyMap())
    }
}