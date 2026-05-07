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

package com.nunchuk.android.wallet.personal.components.stablecoin.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R as CoreR
import com.nunchuk.android.widget.R as WidgetR

const val StablecoinIntroScreenRoute = "stablecoin_intro_screen"

fun NavGraphBuilder.stablecoinIntroScreen(
    onContinueClicked: () -> Unit,
) {
    composable(StablecoinIntroScreenRoute) {
        StablecoinIntroScreen(onContinueClicked = onContinueClicked)
    }
}

fun NavHostController.navigateStablecoinIntro() {
    navigate(StablecoinIntroScreenRoute)
}

@Composable
fun StablecoinIntroScreen(
    modifier: Modifier = Modifier,
    onContinueClicked: () -> Unit = {},
) {
    NcScaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            NcImageAppBar(
                backgroundRes = WidgetR.drawable.bg_wallet_usdt_graphic,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = onContinueClicked,
            ) {
                Text(text = stringResource(CoreR.string.nc_text_continue))
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResource(CoreR.string.nc_stablecoin_wallet),
                style = NunchukTheme.typography.heading,
            )
            Text(
                text = stringResource(CoreR.string.nc_stablecoin_intro_desc),
                style = NunchukTheme.typography.body,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun StablecoinIntroScreenPreview() {
    NunchukTheme {
        StablecoinIntroScreen()
    }
}
