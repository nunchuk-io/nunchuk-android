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

package com.nunchuk.android.wallet.components.coin.detail.ancestry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getBtcFormatDate
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.type.CoinStatus
import com.nunchuk.android.wallet.R

@Composable
fun CoinAncestryRow(
    modifier: Modifier = Modifier,
    showTopLine: Boolean = true,
    showBottomLine: Boolean = true,
    isRootCoin: Boolean = false,
    coins: List<UnspentOutput> = emptyList(),
    onCoinClick: (output: UnspentOutput) -> Unit = {},
) {
    ConstraintLayout(
        modifier = modifier.fillMaxWidth(),
    ) {
        val (topDivider, bottomDivider, list, circle) = createRefs()

        val circleModifier = Modifier.constrainAs(circle) {
            width = Dimension.value(24.dp)
            height = Dimension.value(24.dp)
            top.linkTo(parent.top)
            bottom.linkTo(list.bottom)
            start.linkTo(parent.start)
            end.linkTo(list.start)
        }
        if (isRootCoin) {
            Box(
                modifier = circleModifier
                    .background(
                        color = MaterialTheme.colors.primary,
                        shape = CircleShape
                    )
                    .border(width = 2.dp, shape = CircleShape, color = MaterialTheme.colors.border)
            )
        } else {
            Box(
                modifier = circleModifier.background(
                    color = MaterialTheme.colors.border,
                    shape = CircleShape
                )
            )
        }

        if (showTopLine) {
            Divider(modifier = Modifier.constrainAs(topDivider) {
                width = Dimension.value(2.dp)
                height = Dimension.fillToConstraints
                top.linkTo(parent.top)
                bottom.linkTo(circle.top)
                start.linkTo(parent.start)
                end.linkTo(list.start)
            }, color = MaterialTheme.colors.border)
        }

        if (showBottomLine) {
            Divider(modifier = Modifier.constrainAs(bottomDivider) {
                width = Dimension.value(2.dp)
                height = Dimension.fillToConstraints
                top.linkTo(circle.bottom)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(list.start)
            }, color = MaterialTheme.colors.border)
        }

        LazyRow(
            modifier = Modifier.constrainAs(list) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom, margin = 24.dp)
                start.linkTo(parent.start, margin = 52.dp)
            },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 12.dp)
        ) {
            items(coins) { coin ->
                Card(
                    modifier = Modifier.clickable(
                        enabled = isRootCoin.not(),
                        onClick = { onCoinClick(coin) }),
                    shape = NunchukTheme.shape.medium,
                    border = BorderStroke(width = 1.dp, color = MaterialTheme.colors.border)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = coin.amount.getBTCAmount(),
                                style = NunchukTheme.typography.title
                            )
                            Text(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .border(
                                        width = 1.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colors.border
                                    )
                                    .padding(horizontal = 8.dp),
                                text = if (isRootCoin) stringResource(R.string.nc_wallet_this_coin)
                                else stringResource(R.string.nc_wallet_spent),
                                style = NunchukTheme.typography.bodySmall.copy(fontSize = 10.sp)
                            )
                        }

                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = coin.time.getBtcFormatDate(),
                            style = NunchukTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CoinAncestryRowPreview() {
    val coin = UnspentOutput(
        amount = Amount(1000000L),
        isLocked = true,
        scheduleTime = System.currentTimeMillis(),
        time = System.currentTimeMillis(),
        tags = setOf(),
        memo = "Send to Bob on Silk Road",
        status = CoinStatus.OUTGOING_PENDING_CONFIRMATION
    )
    NunchukTheme {
        CoinAncestryRow(
            coins = listOf(
                coin.copy(vout = 1),
                coin.copy(vout = 2),
                coin.copy(vout = 3),
                coin.copy(vout = 4),
                coin.copy(vout = 5)
            )
        )
    }
}