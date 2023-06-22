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

package com.nunchuk.android.wallet.components.coin.search

import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.wallet.R

@Composable
fun SearchCoinTopAppBar(
    modifier: Modifier = Modifier,
    onBackPressOwner: OnBackPressedDispatcherOwner?,
    query: String,
    isEmpty: Boolean = false,
    onQueryChange: (String) -> Unit,
    enableSelectMode: () -> Unit,
    onFilterClicked: () -> Unit,
    isShowSelect: Boolean = true,
    isFiltering: Boolean = true,
) {
    val isShowClearSearch by remember(query) {
        derivedStateOf { query.isNotEmpty() }
    }
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = { onBackPressOwner?.onBackPressedDispatcher?.onBackPressed() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            if (isShowSelect) {
                AnimatedVisibility(visible = isEmpty.not()) {
                    Text(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { enableSelectMode() },
                        text = stringResource(R.string.nc_select),
                        style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                    )
                }
            }
            Box {
                IconButton(onClick = onFilterClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter),
                        contentDescription = "Filter icon"
                    )
                }
                if (isFiltering) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, end = 8.dp)
                            .size(12.dp)
                            .background(color = MaterialTheme.colors.error, shape = CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        },
        title = {
            TextField(
                modifier = Modifier
                    .background(color = MaterialTheme.colors.background)
                    .border(
                        color = NcColor.border,
                        width = 1.dp,
                        shape = RoundedCornerShape(44.dp),
                    ),
                value = query,
                onValueChange = {
                    onQueryChange(it)
                },
                textStyle = NunchukTheme.typography.body,
                placeholder = {
                    Text(text = stringResource(R.string.nc_search_coins))
                },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = MaterialTheme.colors.background,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                trailingIcon = {
                    if (isShowClearSearch) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Close icon"
                            )
                        }
                    }
                }
            )
        }, elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.background
    )
}