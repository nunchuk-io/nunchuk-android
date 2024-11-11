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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.nunchuk.android.compose

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.nunchuk.android.core.R

@Composable
fun NcImageAppBar(
    modifier: Modifier = Modifier,
    @DrawableRes backgroundRes: Int,
    @DrawableRes backIconRes: Int = R.drawable.ic_back,
    onClosedClicked: (() -> Unit)? = null,
    title: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val onBackPressOwner = LocalOnBackPressedDispatcherOwner.current
    val defaultClosedClick = {
        onBackPressOwner?.onBackPressedDispatcher?.onBackPressed()
        Unit
    }
    val onClick = onClosedClicked ?: defaultClosedClick
    Column(modifier = modifier) {
        Box {
            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(id = backgroundRes),
                contentScale = ContentScale.Crop,
                contentDescription = "Background",
            )
            CenterAlignedTopAppBar(title = {
                if (title != null) {
                    Text(
                        textAlign = TextAlign.Center,
                        text = title,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = NunchukTheme.typography.titleSmall
                    )
                }
            }, navigationIcon = {
                IconButton(onClick = onClick) {
                    NcIcon(
                        painter = painterResource(id = backIconRes),
                        contentDescription = "Back Icon"
                    )
                }
            }, actions = actions,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    }
}

@Preview
@Composable
fun NcImageAppBarPreview() {
    NcImageAppBar(
        backgroundRes = R.drawable.nc_bg_tap_signer_chip,
        title = "Est. time remaining: xx minutes",
    )
}