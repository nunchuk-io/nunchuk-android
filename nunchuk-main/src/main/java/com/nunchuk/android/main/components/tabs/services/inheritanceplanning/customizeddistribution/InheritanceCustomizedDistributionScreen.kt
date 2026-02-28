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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.customizeddistribution

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
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SelectableContainer
import com.nunchuk.android.main.R

internal enum class BeneficiaryType(
    @StringRes val labelRes: Int,
) {
    SINGLE(
        labelRes = R.string.nc_single_beneficiary
    ),
    MULTI(
        labelRes = R.string.nc_multi_beneficiary
    )
}

@Composable
internal fun InheritanceCustomizedDistributionScreen(
    remainTime: Int,
    onContinueClicked: (BeneficiaryType) -> Unit = {},
) {
    var selectedType by rememberSaveable { mutableStateOf<BeneficiaryType?>(null) }
    InheritanceCustomizedDistributionContent(
        remainTime = remainTime,
        selectedType = selectedType,
        onTypeClick = { selectedType = it },
        onContinueClicked = {
            selectedType?.let(onContinueClicked)
        }
    )
}

@Composable
private fun InheritanceCustomizedDistributionContent(
    remainTime: Int = 0,
    selectedType: BeneficiaryType? = null,
    onTypeClick: (BeneficiaryType) -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_inheritance_customize_distribution,
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
                    enabled = selectedType != null,
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
                    text = stringResource(id = R.string.nc_customized_distribution_title),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.nc_customized_distribution_desc),
                    style = NunchukTheme.typography.body
                )
                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = stringResource(id = R.string.nc_customized_distribution_question),
                    style = NunchukTheme.typography.body
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BeneficiaryType.entries.forEach { type ->
                        BeneficiaryOptionItem(
                            modifier = Modifier.fillMaxWidth(),
                            isSelected = selectedType == type,
                            label = stringResource(id = type.labelRes),
                            onClick = { onTypeClick(type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BeneficiaryOptionItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    SelectableContainer(
        modifier = modifier,
        paddingValues = PaddingValues(16.dp),
        borderWidth = 2.dp,
        isSelected = isSelected,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NcRadioButton(
                modifier = Modifier.size(24.dp),
                selected = isSelected,
                onClick = onClick
            )
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = label,
                style = NunchukTheme.typography.title
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceCustomizedDistributionScreenPreview() {
    InheritanceCustomizedDistributionContent(remainTime = 18)
}
