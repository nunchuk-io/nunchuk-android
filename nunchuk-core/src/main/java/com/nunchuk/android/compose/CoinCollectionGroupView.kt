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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoinCollectionGroupView(
    modifier: Modifier = Modifier,
    note: String = "",
    collectionIds: Set<Int>,
    collections: Map<Int, CoinCollection>,
    onViewCollectionDetail: (collection: CoinCollection) -> Unit = {}
) {
    var onCollectionExpand by remember { mutableStateOf(false) }
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
            collectionIds.take(if (onCollectionExpand) Int.MAX_VALUE else 5).mapNotNull { collections[it] }
                .sortedBy { it.name }.forEach { coinCollection ->
                    CoinCollectionHorizontalView(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clickable { onViewCollectionDetail(coinCollection) },
                        collection = coinCollection
                    )
                }
            if (collectionIds.size > 5) {
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
                        .clickable { onCollectionExpand = onCollectionExpand.not() },
                    text = if (onCollectionExpand) stringResource(R.string.nc_show_less) else "${collectionIds.size - 5} more tags",
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
fun CoinCollectionGroupViewPreviewNoNote() {
    NunchukTheme {
        CoinCollectionGroupView(
            modifier = Modifier.padding(16.dp),
            collectionIds = setOf(1, 2, 3, 4, 5, 6, 7),
            collections = mapOf(
                1 to CoinCollection(id = 1, name = "badcoins"),
                2 to CoinCollection(id = 1, name = "badcoins"),
                3 to CoinCollection(id = 1, name = "badcoins"),
                4 to CoinCollection(id = 1, name = "badcoins"),
                5 to CoinCollection(id = 1, name = "badcoins"),
            ),
            note = "",
        )
    }
}