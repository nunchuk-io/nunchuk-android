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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.estimateRemainTimeTitle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillDenim2
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBeneficiaryAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasemethod.InheritanceReleaseMethod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleSummaryProgress
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.view.AllocationDonutChart
import com.nunchuk.android.core.R as CoreR
import com.nunchuk.android.widget.R as WidgetR

@Composable
internal fun InheritanceBeneficiarySchedulesScreen(
    remainTime: Int,
    releaseMethod: InheritanceReleaseMethod,
    beneficiaries: List<InheritanceBeneficiaryAllocation>,
    releaseScheduleUiState: ReleaseScheduleUiState = ReleaseScheduleUiState(),
    individualScheduleCardDataByEmail: Map<String, InheritanceBeneficiaryScheduleCardData> = emptyMap(),
    sharedBufferPeriodSummaryText: String? = null,
    fallbackSummaryText: String? = null,
    isSharedScheduleConfigured: Boolean = false,
    onBackClicked: () -> Unit = {},
    onEditReleaseMethodClicked: () -> Unit = {},
    onAddReleaseScheduleClicked: () -> Unit = {},
    onEditSharedScheduleClicked: () -> Unit = {},
    onEditBeneficiaryScheduleClicked: (String) -> Unit = {},
    onEditFallbackSettingsClicked: () -> Unit = {},
    onEditAssetAllocationClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    val isIndividualSchedulesConfigured = releaseMethod == InheritanceReleaseMethod.INDIVIDUAL_SCHEDULES &&
        beneficiaries.isNotEmpty() &&
        beneficiaries.all { beneficiary ->
            beneficiary.allocationPercent <= 0 ||
                individualScheduleCardDataByEmail.containsKey(beneficiary.email) ||
                individualScheduleCardDataByEmail.containsKey(beneficiary.email.trim().lowercase())
        }

    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = estimateRemainTimeTitle(remainTime),
                    onBackPress = onBackClicked
                )
            },
            bottomBar = {
                BottomActionSection(
                    releaseMethod = releaseMethod,
                    beneficiaries = beneficiaries,
                    isSharedScheduleConfigured = isSharedScheduleConfigured,
                    isIndividualSchedulesConfigured = isIndividualSchedulesConfigured,
                    onEditAssetAllocationClicked = onEditAssetAllocationClicked,
                    onAddReleaseScheduleClicked = onAddReleaseScheduleClicked,
                    onContinueClicked = onContinueClicked
                )
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
                    text = stringResource(id = R.string.nc_beneficiary_schedules_title),
                    style = NunchukTheme.typography.heading
                )

                ReleaseMethodSummaryCard(
                    modifier = Modifier.padding(top = 16.dp),
                    releaseMethod = releaseMethod,
                    onEditClicked = onEditReleaseMethodClicked
                )

                Text(
                    modifier = Modifier.padding(top = 20.dp),
                    text = stringResource(
                        id = if (releaseMethod == InheritanceReleaseMethod.SHARED_SCHEDULE) {
                            R.string.nc_beneficiary_schedules_shared_desc
                        } else {
                            R.string.nc_beneficiary_schedules_individual_desc
                        }
                    ),
                    style = NunchukTheme.typography.body
                )

                if (releaseMethod == InheritanceReleaseMethod.SHARED_SCHEDULE) {
                    if (isSharedScheduleConfigured) {
                        SharedScheduleConfiguredCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            uiState = releaseScheduleUiState,
                            bufferPeriodSummaryText = sharedBufferPeriodSummaryText,
                            onEditClick = onEditSharedScheduleClicked
                        )
                    } else {
                        SharedScheduleEmptyState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 100.dp)
                        )
                    }
                } else {
                    IndividualScheduleList(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        beneficiaries = beneficiaries,
                        individualScheduleCardDataByEmail = individualScheduleCardDataByEmail,
                        onEditBeneficiaryScheduleClicked = onEditBeneficiaryScheduleClicked
                    )
                }

                if (fallbackSummaryText != null) {
                    FallbackSettingsSummaryCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        summaryText = fallbackSummaryText,
                        onEditClicked = onEditFallbackSettingsClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseMethodSummaryCard(
    modifier: Modifier = Modifier,
    releaseMethod: InheritanceReleaseMethod,
    onEditClicked: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.fillDenim2,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.nc_beneficiary_schedules_release_method_prefix),
            style = NunchukTheme.typography.title
        )
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = stringResource(
                id = if (releaseMethod == InheritanceReleaseMethod.SHARED_SCHEDULE) {
                    R.string.nc_release_method_shared_schedule
                } else {
                    R.string.nc_release_method_individual_schedules
                }
            ),
            style = NunchukTheme.typography.body
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.clickable(onClick = onEditClicked),
            text = stringResource(id = CoreR.string.nc_edit),
            style = NunchukTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline)
        )
    }
}

@Composable
private fun SharedScheduleConfiguredCard(
    modifier: Modifier = Modifier,
    uiState: ReleaseScheduleUiState,
    bufferPeriodSummaryText: String?,
    onEditClick: () -> Unit,
) {
    val firstStage = uiState.stages.firstOrNull()
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.strokePrimary,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = WidgetR.drawable.ic_calendar_blank),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textPrimary
                )
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    text = if (firstStage != null) {
                        stringResource(
                            id = R.string.nc_release_schedule_first_withdrawal,
                            firstStage.firstWithdrawalDate.display()
                        )
                    } else {
                        stringResource(id = R.string.nc_beneficiary_schedules_not_set_up_yet)
                    },
                    style = NunchukTheme.typography.body
                )
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onEditClick),
                    painter = painterResource(id = WidgetR.drawable.ic_edit_small),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textPrimary
                )
            }

            if (bufferPeriodSummaryText != null) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = WidgetR.drawable.ic_buffer_period),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.textPrimary
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = bufferPeriodSummaryText,
                        style = NunchukTheme.typography.body
                    )
                }
            }

            ReleaseScheduleSummaryProgress(
                modifier = Modifier.padding(top = 12.dp),
                segments = uiState.allocationSegments,
                summaryScalePercent = uiState.summaryScalePercent,
                remainingSummaryPercent = uiState.remainingSummaryPercent,
            )
        }
    }
}

@Composable
private fun SharedScheduleEmptyState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(60.dp),
            painter = painterResource(id = WidgetR.drawable.ic_plus_square),
            contentDescription = null,
            tint = Color.Unspecified
        )
        NcHighlightText(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            text = stringResource(id = R.string.nc_beneficiary_schedules_empty_hint),
            style = NunchukTheme.typography.body.copy(
                color = MaterialTheme.colorScheme.textSecondary,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun IndividualScheduleList(
    modifier: Modifier = Modifier,
    beneficiaries: List<InheritanceBeneficiaryAllocation>,
    individualScheduleCardDataByEmail: Map<String, InheritanceBeneficiaryScheduleCardData>,
    onEditBeneficiaryScheduleClicked: (String) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        beneficiaries.filter { it.allocationPercent > 0 }.forEach { beneficiary ->
            val beneficiaryKey = beneficiary.email.trim().lowercase()
            val scheduleCardData = individualScheduleCardDataByEmail[beneficiary.email]
                ?: individualScheduleCardDataByEmail[beneficiaryKey]
            val firstStage = scheduleCardData?.releaseScheduleUiState?.stages?.firstOrNull()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.strokePrimary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = beneficiary.email,
                            style = NunchukTheme.typography.title
                        )
                        Icon(
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { onEditBeneficiaryScheduleClicked(beneficiary.email) },
                            painter = painterResource(id = WidgetR.drawable.ic_edit_small),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.textPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = WidgetR.drawable.ic_calendar_blank),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.textPrimary
                        )
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = if (firstStage == null) {
                                stringResource(id = R.string.nc_beneficiary_schedules_not_set_up_yet)
                            } else {
                                stringResource(
                                    id = R.string.nc_release_schedule_first_withdrawal,
                                    firstStage.firstWithdrawalDate.display()
                                )
                            },
                            style = NunchukTheme.typography.body.copy(
                                color = if (firstStage == null) {
                                    MaterialTheme.colorScheme.textSecondary
                                } else {
                                    MaterialTheme.colorScheme.textPrimary
                                }
                            )
                        )
                    }

                    if (firstStage != null && scheduleCardData.bufferPeriodSummaryText != null) {
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = WidgetR.drawable.ic_buffer_period),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.textPrimary
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = scheduleCardData.bufferPeriodSummaryText,
                                style = NunchukTheme.typography.body
                            )
                        }
                    }

                    if (firstStage != null) {
                        ReleaseScheduleSummaryProgress(
                            modifier = Modifier.padding(top = 12.dp),
                            segments = scheduleCardData.releaseScheduleUiState.allocationSegments,
                            summaryScalePercent = scheduleCardData.releaseScheduleUiState.summaryScalePercent,
                            remainingSummaryPercent = scheduleCardData.releaseScheduleUiState.remainingSummaryPercent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomActionSection(
    releaseMethod: InheritanceReleaseMethod,
    beneficiaries: List<InheritanceBeneficiaryAllocation>,
    isSharedScheduleConfigured: Boolean,
    isIndividualSchedulesConfigured: Boolean,
    onEditAssetAllocationClicked: () -> Unit,
    onAddReleaseScheduleClicked: () -> Unit,
    onContinueClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.greyLight)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .navigationBarsPadding()
    ) {
        AssetAllocationSection(
            beneficiaries = beneficiaries,
            onEditClicked = onEditAssetAllocationClicked,
        )

        if (releaseMethod == InheritanceReleaseMethod.SHARED_SCHEDULE && !isSharedScheduleConfigured) {
            NcOutlineButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                onClick = onAddReleaseScheduleClicked,
            ) {
                Icon(
                    painter = painterResource(id = WidgetR.drawable.ic_add_2),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(id = R.string.nc_add_release_schedule)
                )
            }
        }

        NcPrimaryDarkButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            enabled = when (releaseMethod) {
                InheritanceReleaseMethod.SHARED_SCHEDULE -> isSharedScheduleConfigured
                InheritanceReleaseMethod.INDIVIDUAL_SCHEDULES -> isIndividualSchedulesConfigured
            },
            onClick = onContinueClicked,
        ) {
            Text(text = stringResource(id = R.string.nc_text_continue))
        }
    }
}

@Composable
private fun FallbackSettingsSummaryCard(
    modifier: Modifier = Modifier,
    summaryText: String,
    onEditClicked: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.lightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.nc_fallback_settings_title_with_colon),
                style = NunchukTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier.clickable(onClick = onEditClicked),
                text = stringResource(id = CoreR.string.nc_edit),
                style = NunchukTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline)
            )
        }

        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = summaryText,
            style = NunchukTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AssetAllocationSection(
    beneficiaries: List<InheritanceBeneficiaryAllocation>,
    onEditClicked: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.nc_asset_allocation),
                style = NunchukTheme.typography.title
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier.clickable(onClick = onEditClicked),
                text = stringResource(id = CoreR.string.nc_edit),
                style = NunchukTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline)
            )
        }
        if (beneficiaries.isNotEmpty()) {
            AllocationDonutChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                beneficiaries = beneficiaries,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceBeneficiarySchedulesSharedPreview() {
    InheritanceBeneficiarySchedulesScreen(
        remainTime = 20,
        releaseMethod = InheritanceReleaseMethod.SHARED_SCHEDULE,
        beneficiaries = listOf(
            InheritanceBeneficiaryAllocation("Wife@gmail.com", 50),
            InheritanceBeneficiaryAllocation("Son@gmail.com", 25),
            InheritanceBeneficiaryAllocation("Daughter@gmail.com", 25),
        ),
        isSharedScheduleConfigured = true,
    )
}

@PreviewLightDark
@Composable
private fun InheritanceBeneficiarySchedulesIndividualPreview() {
    InheritanceBeneficiarySchedulesScreen(
        remainTime = 20,
        releaseMethod = InheritanceReleaseMethod.INDIVIDUAL_SCHEDULES,
        beneficiaries = listOf(
            InheritanceBeneficiaryAllocation("Wife@gmail.com", 50),
            InheritanceBeneficiaryAllocation("Son@gmail.com", 25),
            InheritanceBeneficiaryAllocation("Daughter@gmail.com", 25),
        ),
        individualScheduleCardDataByEmail = mapOf(
            "Wife@gmail.com" to InheritanceBeneficiaryScheduleCardData(
                releaseScheduleUiState = ReleaseScheduleUiState(
                    stages = ReleaseScheduleUiState.largeDataPreviewStages()
                ),
                bufferPeriodSummaryText = "Buffer period: 7 days (first withdrawal only)",
            ),
        ),
    )
}
