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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R
import com.nunchuk.android.model.CoinTag

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoinTagGroupView(
    modifier: Modifier = Modifier,
    note: String = "",
    tagIds: Set<Int>,
    tags: Map<Int, CoinTag>,
    onViewTagDetail: (tag: CoinTag) -> Unit = {}
) {
    var onTagExpand by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NcColor.border, RoundedCornerShape(12.dp))
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            maxItemsInEachRow = 4
        ) {
            tagIds.take(if (onTagExpand) Int.MAX_VALUE else 5).mapNotNull { tags[it] }
                .sortedBy { it.name }.forEach { coinTag ->
                    CoinTagView(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clickable { onViewTagDetail(coinTag) },
                        tag = coinTag
                    )
                }
            if (tagIds.size > 5) {
                Text(
                    modifier = Modifier
                        .padding(top = 4.dp, start = 4.dp, end = 8.dp)
                        .background(
                            color = NcColor.greyLight,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = NcColor.border,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable { onTagExpand = onTagExpand.not() },
                    text = if (onTagExpand) stringResource(R.string.nc_show_less) else "${tagIds.size - 5} more tags",
                    style = NunchukTheme.typography.bodySmall,
                )
            }
        }
        if (note.isNotEmpty()) {
            TransactionNoteView(modifier = Modifier.padding(horizontal = 8.dp), note = note)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CoinTagGroupViewPreview() {
    NunchukTheme {
        CoinTagGroupView(
            modifier = Modifier.padding(16.dp),
            tagIds = setOf(1, 2, 3, 4, 5, 6, 7),
            tags = mapOf(
                1 to CoinTag(id = 1, name = "badcoins", color = "#000000"),
                2 to CoinTag(id = 2, name = "badcoins", color = "#000000"),
                3 to CoinTag(id = 3, name = "badcoins", color = "#000000"),
                4 to CoinTag(id = 4, name = "badcoins", color = "#000000"),
                5 to CoinTag(id = 5, name = "badcoins", color = "#000000"),
                6 to CoinTag(id = 6, name = "badcoins", color = "#000000"),
                7 to CoinTag(id = 7, name = "badcoins", color = "#000000"),
            ),
            note = "Send to Bob on Silk Road"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CoinTagGroupViewPreviewNoNote() {
    NunchukTheme {
        CoinTagGroupView(
            modifier = Modifier.padding(16.dp),
            tagIds = setOf(1, 2, 3, 4, 5, 6, 7),
            tags = mapOf(
                1 to CoinTag(id = 1, name = "badcoins", color = "#DDFFFF"),
                2 to CoinTag(id = 2, name = "badcoins", color = "#000000"),
                3 to CoinTag(id = 3, name = "badcoins", color = "#000000"),
                4 to CoinTag(id = 4, name = "badcoins", color = "#000000"),
                5 to CoinTag(id = 5, name = "badcoins", color = "#000000"),
                6 to CoinTag(id = 6, name = "badcoins", color = "#000000"),
                7 to CoinTag(id = 7, name = "badcoins", color = "#000000"),
            ),
            note = "",
        )
    }
}