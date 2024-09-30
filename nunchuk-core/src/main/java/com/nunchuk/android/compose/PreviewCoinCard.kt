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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.formatDate
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.type.CoinStatus

const val MODE_VIEW_ONLY = 1
const val MODE_VIEW_DETAIL = 2
const val MODE_SELECT = 3

@Composable
fun PreviewCoinCard(
    modifier: Modifier = Modifier,
    output: UnspentOutput,
    tags: Map<Int, CoinTag>,
    mode: Int = MODE_VIEW_DETAIL,
    isSelected: Boolean = false,
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onViewTagDetail: (tag: CoinTag) -> Unit = {},
    onSelectCoin: (output: UnspentOutput, isSelected: Boolean) -> Unit = { _, _ -> }
) {
    Box(modifier = modifier
        .run {
        if (mode == MODE_VIEW_DETAIL) {
            this.clickable { onViewCoinDetail(output) }
        } else {
            this
        }
    }) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (LocalView.current.isInEditMode)
                        "${output.amount.value} sats"
                    else
                        output.amount.getBTCAmount(),
                    style = NunchukTheme.typography.title
                )
                if (output.isChange && mode != MODE_VIEW_ONLY) {
                    Text(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(
                                1.dp,
                                color = NcColor.whisper,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        text = stringResource(R.string.nc_change),
                        style = NunchukTheme.typography.titleSmall.copy(fontSize = 10.sp)
                    )
                }
                if (output.isLocked && mode != MODE_VIEW_ONLY) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = colorResource(id = R.color.nc_whisper_color),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .size(12.dp),
                        painter = painterResource(id = R.drawable.ic_lock),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "Lock"
                    )
                }
                if (output.scheduleTime > 0L && mode != MODE_VIEW_ONLY) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = colorResource(id = R.color.nc_whisper_color),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .size(12.dp),
                        painter = painterResource(id = R.drawable.ic_schedule),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "Schedule"
                    )
                }
            }
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = output.amount.getCurrencyAmount(),
                style = NunchukTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (output.time > 0L) {
                    Text(
                        text = output.time.formatDate(),
                        style = NunchukTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "--/--/--",
                        style = NunchukTheme.typography.bodySmall
                    )
                }

                if (mode != MODE_VIEW_ONLY || output.status == CoinStatus.INCOMING_PENDING_CONFIRMATION ) {
                    CoinStatusBadge(output)
                }
            }

            if (output.tags.isNotEmpty() || output.memo.isNotEmpty()) {
                CoinTagGroupView(
                    modifier = Modifier.padding(top = 4.dp),
                    note = output.memo,
                    tagIds = output.tags,
                    tags = tags,
                    onViewTagDetail = onViewTagDetail
                )
            }
        }
        if (mode == MODE_SELECT) {
            Checkbox(modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp),
                checked = isSelected,
                onCheckedChange = { select ->
                    onSelectCoin(output, select)
                })
        } else if (mode == MODE_VIEW_DETAIL) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp),
                onClick = { onViewCoinDetail(output) }) {
                Icon(painter = painterResource(id = R.drawable.ic_arrow), contentDescription = "", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCoinCardPreview() {
    NunchukTheme {
        PreviewCoinCard(
            output = UnspentOutput(
                amount = Amount(1000000L),
                isLocked = true,
                scheduleTime = System.currentTimeMillis(),
                isChange = true,
                time = System.currentTimeMillis(),
                tags = setOf(1, 2, 3, 4),
                memo = "Send to Bob on Silk Road",
                status = CoinStatus.OUTGOING_PENDING_CONFIRMATION
            ),
            tags = emptyMap()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCoinCardPreview2() {
    NunchukTheme {
        PreviewCoinCard(
            output = UnspentOutput(
                amount = Amount(1000000L),
                isLocked = false,
                scheduleTime = System.currentTimeMillis(),
                time = System.currentTimeMillis(),
                tags = setOf(),
                memo = "",
                status = CoinStatus.OUTGOING_PENDING_CONFIRMATION
            ),
            tags = emptyMap()
        )
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewCoinCardPreview3() {
    NunchukTheme {
        PreviewCoinCard(
            modifier = Modifier
                .background(color = Color.White, shape = RoundedCornerShape(12.dp)),
            output = UnspentOutput(
                amount = Amount(1000000L),
                isLocked = false,
                scheduleTime = System.currentTimeMillis() / 1000,
                time = System.currentTimeMillis() / 1000,
                tags = setOf(),
                memo = "",
                status = CoinStatus.OUTGOING_PENDING_CONFIRMATION
            ),
            tags = emptyMap(),
            mode = MODE_SELECT,
            isSelected = true
        )
    }
}