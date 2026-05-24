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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.planoverview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceSetupFlowType
import com.nunchuk.android.model.byzantine.GroupWalletType

@Composable
internal fun InheritancePlanOverviewScreen(
    viewModel: InheritancePlanOverviewViewModel = viewModel(),
    groupWalletType: GroupWalletType? = null,
    isMiniscriptWallet: Boolean,
    setupFlowType: InheritanceSetupFlowType = InheritanceSetupFlowType.OLD_FLOW,
    onContinueClicked: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    InheritancePlanOverviewContent(
        isMiniscriptWallet = isMiniscriptWallet,
        remainTime = remainTime,
        groupWalletType = groupWalletType,
        setupFlowType = setupFlowType,
        onContinueClicked = onContinueClicked
    )
}

@Composable
private fun InheritancePlanOverviewContent(
    isMiniscriptWallet: Boolean,
    remainTime: Int = 0,
    groupWalletType: GroupWalletType? = null,
    setupFlowType: InheritanceSetupFlowType = InheritanceSetupFlowType.OLD_FLOW,
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    }
                )
            }
        ) { innerPadding ->
            Column(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_plan_overview),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = if (!isMiniscriptWallet && setupFlowType == InheritanceSetupFlowType.MULTI_BENEFICIARY) {
                        stringResource(R.string.nc_plan_overview_multi_beneficiary_desc)
                    } else {
                        stringResource(R.string.nc_plan_overview_desc)
                    },
                    style = NunchukTheme.typography.body
                )
                if (isMiniscriptWallet) {
                    MiniscriptOverviewItems(groupWalletType = groupWalletType)
                } else when (setupFlowType) {
                    InheritanceSetupFlowType.OLD_FLOW -> OldFlowOverviewItems(groupWalletType = groupWalletType)
                    InheritanceSetupFlowType.SINGLE_BENEFICIARY -> SingleBeneficiaryOverviewItems(
                        groupWalletType = groupWalletType
                    )

                    InheritanceSetupFlowType.MULTI_BENEFICIARY -> MultiBeneficiaryOverviewItems()
                }

                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Composable
private fun MiniscriptOverviewItems(groupWalletType: GroupWalletType? = null) {
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 1,
        label = stringResource(R.string.nc_a_magical_phrase),
    )
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 2,
        label = if (groupWalletType == GroupWalletType.THREE_OF_FIVE_INHERITANCE) {
            stringResource(R.string.nc_two_inheritance_keys)
        } else {
            stringResource(R.string.nc_an_inheritance_key)
        },
    )
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 3,
        label = stringResource(R.string.nc_an_on_chain_timelock),
    )
    Text(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        text = stringResource(R.string.nc_plan_overview_bottom_desc),
        style = NunchukTheme.typography.body
    )
}

@Composable
private fun OldFlowOverviewItems(groupWalletType: GroupWalletType? = null) {
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 1,
        label = stringResource(R.string.nc_a_magical_phrase),
    )
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 2,
        label = if (groupWalletType == GroupWalletType.THREE_OF_FIVE_INHERITANCE) {
            stringResource(id = R.string.nc_two_backup_password)
        } else {
            stringResource(id = R.string.nc_a_backup_password)
        },
    )
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 3,
        label = stringResource(R.string.nc_an_off_chain_timelock),
    )
    Text(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        text = stringResource(R.string.nc_plan_overview_bottom_desc),
        style = NunchukTheme.typography.body
    )
}

@Composable
private fun SingleBeneficiaryOverviewItems(groupWalletType: GroupWalletType? = null) {
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 1,
        label = stringResource(R.string.nc_plan_overview_magic_phrase),
    )
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 2,
        label = if (groupWalletType == GroupWalletType.THREE_OF_FIVE_INHERITANCE) {
            stringResource(id = R.string.nc_two_backup_password)
        } else {
            stringResource(id = R.string.nc_plan_overview_backup_password)
        },
    )
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 3,
        label = stringResource(R.string.nc_release_schedule),
    )
}

@Composable
private fun MultiBeneficiaryOverviewItems() {
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 1,
        label = stringResource(R.string.nc_asset_allocation),
    )
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 2,
        label = stringResource(R.string.nc_release_schedules),
    )
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 3,
        label = stringResource(R.string.nc_magic_phrases),
    )
    NCLabelWithIndex(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        index = 4,
        label = stringResource(R.string.nc_plan_overview_backup_password),
    )
}

@PreviewLightDark
@Composable
private fun InheritancePlanOverviewScreenPreview() {
    InheritancePlanOverviewContent(isMiniscriptWallet = false)
}

@PreviewLightDark
@Composable
private fun InheritancePlanOverviewScreenMultiBeneficiaryPreview() {
    InheritancePlanOverviewContent(
        isMiniscriptWallet = false,
        setupFlowType = InheritanceSetupFlowType.MULTI_BENEFICIARY
    )
}

@PreviewLightDark
@Composable
private fun InheritancePlanOverviewScreenMiniscriptPreview() {
    InheritancePlanOverviewContent(isMiniscriptWallet = true)
}
