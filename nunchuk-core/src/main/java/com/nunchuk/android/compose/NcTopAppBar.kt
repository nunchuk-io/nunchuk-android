/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun NcTopAppBar(
    title: String,
    hasAction: Boolean = false,
    textStyle: TextStyle = NunchukTheme.typography.titleSmall,
    actions: @Composable RowScope.() -> Unit = {},
    isBack: Boolean = true,
    backgroundColor: Color = MaterialTheme.colors.surface
) {
    val onBackPressOwner = LocalOnBackPressedDispatcherOwner.current
    TopAppBar(
        backgroundColor = backgroundColor,
        elevation = 0.dp,
        navigationIcon = {
            IconButton(onClick = { onBackPressOwner?.onBackPressedDispatcher?.onBackPressed() }) {
                Icon(
                    painter = painterResource(id = if (isBack) R.drawable.ic_back else R.drawable.ic_close),
                    contentDescription = "Back"
                )
            }
        },
        title = {
            Text(
                text = title,
                style = textStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(end = if (hasAction) 0.dp else LocalViewConfiguration.current.minimumTouchTargetSize.width)
                    .fillMaxWidth(),
            )
        },
        actions = actions
    )
}

@Preview
@Composable
fun NcTopAppBarPreview() {
    NcTopAppBar(
        hasAction = true,
        title = "Est. time remaining: xx minutes",
    )
}