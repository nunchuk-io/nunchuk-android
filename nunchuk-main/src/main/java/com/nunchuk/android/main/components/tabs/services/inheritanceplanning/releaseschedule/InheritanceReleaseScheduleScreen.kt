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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R

@Composable
internal fun InheritanceReleaseScheduleScreen(
    remainTime: Int,
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_inheritance_release_schedule,
                    backIconRes = com.nunchuk.android.core.R.drawable.ic_close,
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_setup_release_schedule_title),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(id = R.string.nc_setup_release_schedule_desc_1),
                        style = NunchukTheme.typography.body
                    )
                    Text(
                        modifier = Modifier.padding(top = 24.dp),
                        text = stringResource(id = R.string.nc_setup_release_schedule_desc_2),
                        style = NunchukTheme.typography.body
                    )
                    Text(
                        modifier = Modifier.padding(top = 24.dp),
                        text = stringResource(id = R.string.nc_setup_release_schedule_desc_3),
                        style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceReleaseScheduleScreenPreview() {
    InheritanceReleaseScheduleScreen(remainTime = 12)
}
