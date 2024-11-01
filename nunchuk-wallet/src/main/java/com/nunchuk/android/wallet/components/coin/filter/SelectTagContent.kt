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

package com.nunchuk.android.wallet.components.coin.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.TagItem
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.wallet.R

@Composable
fun SelectTagContent(
    tags: List<CoinTagAddition> = emptyList(),
    selectedCoinTags: Set<Int> = emptySet(),
    onBackPressed: () -> Unit = {},
    onCheckedChange: ((Int, Boolean) -> Unit) = { _, _ -> },
    onSelectDone: () -> Unit = {},
    onSelectOrUnselectAll: (isSelect: Boolean) -> Unit = {},
) {
    val isSelectAll = selectedCoinTags.size == tags.size
    NunchukTheme {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.background)
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                IconButton(modifier = Modifier.align(Alignment.CenterStart), onClick = {
                    onBackPressed()
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back icon"
                    )
                }
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.nc_select_coins),
                    style = NunchukTheme.typography.titleLarge
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .clickable { onSelectOrUnselectAll(isSelectAll.not()) },
                    text = if (isSelectAll) stringResource(R.string.nc_unselect_all) else stringResource(
                        R.string.nc_select_all
                    ),
                    style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1.0f), verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tags) { tag ->
                    TagItem(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        name = tag.coinTag.name,
                        color = tag.coinTag.color,
                        numCoins = tag.numCoins,
                        checked = selectedCoinTags.contains(tag.coinTag.id),
                        tagFlow = TagFlow.ADD,
                        onTagClick = { },
                        onCheckedChange = {
                            onCheckedChange(tag.coinTag.id, it)
                        })
                }
            }

            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = onSelectDone,
            ) {
                Text(text = stringResource(R.string.nc_apply))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectTagContentPreview() {
    SelectTagContent()
}
