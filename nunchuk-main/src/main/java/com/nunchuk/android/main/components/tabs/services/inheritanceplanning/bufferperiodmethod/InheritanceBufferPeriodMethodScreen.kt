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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiodmethod

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.estimateRemainTimeTitle
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SelectableContainer
import com.nunchuk.android.main.R

internal enum class BufferPeriodMethodOption(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
) {
    FIRST_WITHDRAWAL_ONLY(
        titleRes = R.string.nc_buffer_period_first_withdrawal_only,
        descRes = R.string.nc_buffer_period_first_withdrawal_only_desc,
    ),
    EVERY_WITHDRAWAL(
        titleRes = R.string.nc_buffer_period_every_withdrawal,
        descRes = R.string.nc_buffer_period_every_withdrawal_desc,
    ),
}

@Composable
internal fun InheritanceBufferPeriodMethodScreen(
    remainTime: Int,
    selectedOption: BufferPeriodMethodOption = BufferPeriodMethodOption.FIRST_WITHDRAWAL_ONLY,
    onBackClicked: () -> Unit = {},
    onContinueClicked: (BufferPeriodMethodOption) -> Unit = {},
) {
    var selectedOptionState by rememberSaveable(selectedOption) { mutableStateOf(selectedOption) }
    InheritanceBufferPeriodMethodContent(
        remainTime = remainTime,
        selectedOption = selectedOptionState,
        onBackClicked = onBackClicked,
        onOptionClick = { selectedOptionState = it },
        onContinueClicked = { onContinueClicked(selectedOptionState) },
    )
}

@Composable
private fun InheritanceBufferPeriodMethodContent(
    remainTime: Int = 0,
    selectedOption: BufferPeriodMethodOption = BufferPeriodMethodOption.FIRST_WITHDRAWAL_ONLY,
    onBackClicked: () -> Unit = {},
    onOptionClick: (BufferPeriodMethodOption) -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = estimateRemainTimeTitle(remainTime),
                    onBackPress = onBackClicked
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
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.nc_set_up_buffer_period),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.nc_buffer_period_apply_desc),
                    style = NunchukTheme.typography.body
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BufferPeriodMethodOption.entries.forEach { option ->
                        BufferPeriodMethodItem(
                            modifier = Modifier.fillMaxWidth(),
                            isSelected = selectedOption == option,
                            title = stringResource(id = option.titleRes),
                            description = stringResource(id = option.descRes),
                            onClick = { onOptionClick(option) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BufferPeriodMethodItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    SelectableContainer(
        modifier = modifier,
        paddingValues = PaddingValues(16.dp),
        borderWidth = 2.dp,
        isSelected = isSelected,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            NcRadioButton(
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp),
                selected = isSelected,
                onClick = onClick
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth()
            ) {
                Text(text = title, style = NunchukTheme.typography.title)
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = description,
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceBufferPeriodMethodScreenPreview() {
    InheritanceBufferPeriodMethodContent(remainTime = 20)
}
