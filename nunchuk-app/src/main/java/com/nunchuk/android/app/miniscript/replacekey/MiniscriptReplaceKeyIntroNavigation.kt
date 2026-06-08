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

package com.nunchuk.android.app.miniscript.replacekey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.main.R
import kotlinx.serialization.Serializable

@Serializable
data class MiniscriptReplaceIntro(
    val isReplaceKeyWithTimelock: Boolean = false,
    val walletId: String = ""
)

fun NavGraphBuilder.miniscriptReplaceKeyIntroDestination(
    onContinueClicked: (String) -> Unit = {}
) {
    composable<MiniscriptReplaceIntro> { navBackStackEntry ->
        val route: MiniscriptReplaceIntro = navBackStackEntry.toRoute()
        ReplaceKeyIntroScreen(
            isReplaceKeyWithTimelock = route.isReplaceKeyWithTimelock,
            onContinueClicked = { onContinueClicked(route.walletId) },
        )
    }
}

@Composable
fun ReplaceKeyIntroScreen(
    isLoading: Boolean = false,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onContinueClicked: () -> Unit = {},
    isReplaceKeyWithTimelock: Boolean = false,
) = NunchukTheme {
    if (isLoading) {
        NcLoadingDialog()
    }

    val titleRes = if (isReplaceKeyWithTimelock) {
        R.string.nc_replace_key_change_timelock
    } else {
        R.string.nc_replace_key_title
    }

    val descriptionRes = if (isReplaceKeyWithTimelock) {
        R.string.nc_replace_key_change_timelock_desc
    } else {
        R.string.nc_replace_key_intro_desc_simple
    }

    val noteRes = if (isReplaceKeyWithTimelock) {
        R.string.nc_replace_key_change_timelock_note
    } else {
        R.string.nc_replace_key_intro_note_simple
    }

    NcScaffold(
        modifier = Modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.nc_bg_roll_over_illustrations,
                backIconRes = R.drawable.ic_close
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinueClicked
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(id = titleRes),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                text = stringResource(descriptionRes),
                style = NunchukTheme.typography.body
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                text = stringResource(noteRes),
                style = NunchukTheme.typography.body
            )
        }
    }
}

@Preview
@Composable
fun ReplaceKeyIntroScreenPreview() {
    ReplaceKeyIntroScreen()
}

