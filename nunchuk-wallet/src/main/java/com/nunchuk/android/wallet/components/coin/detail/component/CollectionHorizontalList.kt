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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.CoinCollectionView
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.R

@Composable
fun CollectionHorizontalList(
    modifier: Modifier = Modifier,
    output: UnspentOutput,
    onUpdateCollection: (output: UnspentOutput) -> Unit,
    coinCollections: Map<Int, CoinCollection>,
    onViewCollectionDetail: (collection: CoinCollection) -> Unit
) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp),
            painter = painterResource(id = R.drawable.ic_coin_collection),
            contentDescription = "Lock icon"
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(R.string.nc_collections),
            style = NunchukTheme.typography.title
        )
        if (output.collection.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "(${output.collection.size})",
                style = NunchukTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (output.tags.isNotEmpty()) {
            Text(
                modifier = Modifier.clickable { onUpdateCollection(output) },
                text = stringResource(id = R.string.nc_edit),
                style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline),
            )
        } else {
            Icon(
                modifier = Modifier.clickable { onUpdateCollection(output) },
                imageVector = Icons.Default.Add,
                contentDescription = "Add"
            )
        }
    }
    if (output.collection.isNotEmpty()) {
        val collections = output.collection.mapNotNull {
            coinCollections[it]
        }.sortedBy { it.name }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(collections) { collection ->
                CoinCollectionView(
                    collection = collection,
                    textStyle = NunchukTheme.typography.body,
                    clickable = true
                ) {
                    onViewCollectionDetail(collection)
                }
            }
        }
    } else {
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.nc_coin_has_no_collections),
            textAlign = TextAlign.Center,
            style = NunchukTheme.typography.body
        )
    }
}

@Composable
fun CollectionHorizontalList(
    modifier: Modifier = Modifier,
    collections: List<CoinCollection>,
    onViewAll: () -> Unit,
) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp),
            painter = painterResource(id = R.drawable.ic_coin_collection),
            contentDescription = "Lock icon"
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(R.string.nc_collections),
            style = NunchukTheme.typography.title
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.clickable { onViewAll() },
            text = stringResource(R.string.nc_view_all),
            style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline),
        )
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(collections) { collection ->
            CoinCollectionView(
                collection = collection,
                textStyle = NunchukTheme.typography.body,
                clickable = false
            )
        }
    }
}