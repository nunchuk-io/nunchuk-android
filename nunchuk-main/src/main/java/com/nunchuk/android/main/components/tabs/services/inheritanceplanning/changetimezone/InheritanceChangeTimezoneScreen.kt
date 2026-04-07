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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.changetimezone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.timezone.NcTimeZoneField
import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.core.ui.toTimeZoneDetail
import com.nunchuk.android.main.R
import java.util.TimeZone

@Composable
internal fun InheritanceChangeTimezoneScreen(
    remainTime: Int,
    selectedZoneId: String,
    isUpdateRequest: Boolean,
    onBackClicked: () -> Unit = {},
    onSaveClicked: (String) -> Unit = {},
) {
    var selectedTimeZone by remember(selectedZoneId) {
        mutableStateOf(
            selectedZoneId.toTimeZoneDetail()
                ?: TimeZone.getDefault().id.toTimeZoneDetail()
                ?: TimeZoneDetail()
        )
    }
    val hasChanges = selectedTimeZone.id != selectedZoneId
    InheritanceChangeTimezoneScreenContent(
        remainTime = remainTime,
        isUpdateRequest = isUpdateRequest,
        selectedTimeZone = selectedTimeZone,
        hasChanges = hasChanges,
        onTimeZoneSelected = { selectedTimeZone = it },
        onBackClicked = onBackClicked,
        onSaveClicked = { onSaveClicked(selectedTimeZone.id) },
    )
}

@Composable
private fun InheritanceChangeTimezoneScreenContent(
    remainTime: Int = 0,
    isUpdateRequest: Boolean = true,
    selectedTimeZone: TimeZoneDetail = TimeZone.getDefault().id.toTimeZoneDetail() ?: TimeZoneDetail(),
    hasChanges: Boolean = true,
    onTimeZoneSelected: (TimeZoneDetail) -> Unit = {},
    onBackClicked: () -> Unit = {},
    onSaveClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_estimate_remain_time, remainTime),
                    isBack = !isUpdateRequest,
                    onBackPress = onBackClicked,
                    actions = {
                        Spacer(
                            modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize)
                        )
                    },
                )
            },
            bottomBar = {
                Column {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        enabled = if (isUpdateRequest) hasChanges else true,
                        onClick = onSaveClicked,
                    ) {
                        Text(
                            text = if (isUpdateRequest) {
                                stringResource(id = R.string.nc_save)
                            } else {
                                stringResource(id = R.string.nc_text_continue)
                            }
                        )
                    }
                    if (isUpdateRequest) {
                        NcOutlineButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                                .height(48.dp),
                            onClick = onBackClicked,
                        ) {
                            Text(text = stringResource(id = R.string.nc_cancel))
                        }
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringResource(id = R.string.nc_change_timezone_title),
                    style = NunchukTheme.typography.heading
                )

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.nc_change_timezone_desc),
                    style = NunchukTheme.typography.body
                )

                NcTimeZoneField(
                    modifier = Modifier.padding(top = 16.dp),
                    selectedTimeZone = selectedTimeZone,
                    onTimeZoneSelected = onTimeZoneSelected
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceChangeTimezoneScreenPreview() {
    InheritanceChangeTimezoneScreenContent()
}

