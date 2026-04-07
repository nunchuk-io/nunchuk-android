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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillDenim
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBeneficiaryAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBufferPeriodApplyType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningParam
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceReleaseMethodType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceSetupFlowType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbackSettingsSummaryText
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseScheduleBufferPeriodSummaryText
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules.InheritanceBeneficiaryScheduleCardData
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseInstallmentConfig
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseInstallmentFrequency
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleStage
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleSummaryProgress
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.view.AllocationDonutChart
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.TimelockBased
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.inheritance.EmailNotificationSettings
import com.nunchuk.android.model.inheritance.InheritancePlanBeneficiary
import com.nunchuk.android.utils.Utils
import com.nunchuk.android.widget.R as WidgetR


@Composable
fun InheritanceReviewPlanScreen(
    viewModel: InheritanceReviewPlanViewModel = viewModel(),
    inheritanceViewModel: InheritancePlanningViewModel,
    releaseScheduleUiState: ReleaseScheduleUiState = ReleaseScheduleUiState(stages = emptyList()),
    onEditActivationDateClick: (date: Long) -> Unit,
    onEditNoteClick: (note: String) -> Unit,
    onNotifyPrefClick: (isNotifyToday: Boolean, emails: List<String>) -> Unit,
    onDiscardChange: () -> Unit,
    onShareSecretClicked: () -> Unit,
    onActionTopBarClick: () -> Unit,
    onViewClaimingInstruction: () -> Unit = {},
    onEditBufferPeriodClick: (bufferPeriod: Period?) -> Unit = {},
    onBackUpPasswordInfoClick: () -> Unit = {},
    onEditAssetAllocationClick: () -> Unit = {},
    onEditReleaseMethodClick: () -> Unit = {},
    onEditBeneficiarySchedulesClick: () -> Unit = {},
    onEditFallbackSettingsClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val isContinueButtonEnabled by viewModel.isContinueButtonEnabled.collectAsStateWithLifecycle()
    val sharedUiState by inheritanceViewModel.state.collectAsStateWithLifecycle()
    val setupOrReviewParam = sharedUiState.setupOrReviewParam
    val initialSetupOrReviewParam = sharedUiState.initialSetupOrReviewParam

    LaunchedEffect(setupOrReviewParam, initialSetupOrReviewParam) {
        viewModel.update(
            param = setupOrReviewParam,
            initialParam = initialSetupOrReviewParam,
        )
    }

    InheritanceReviewPlanScreenContent(
        isMiniscriptWallet = sharedUiState.isMiniscriptWallet,
        userEmail = sharedUiState.userEmail,
        remainTime = remainTime,
        planFlow = setupOrReviewParam.planFlow,
        magicalPhrase = setupOrReviewParam.magicalPhrase,
        groupId = setupOrReviewParam.groupId,
        releaseScheduleUiState = releaseScheduleUiState,
        setupOrReviewParam = setupOrReviewParam,
        state = state,
        isContinueButtonEnabled = isContinueButtonEnabled,
        onContinueClicked = {
            viewModel.calculateRequiredSignatures(
                flow = InheritanceReviewPlanViewModel.ReviewFlow.CREATE_OR_UPDATE,
                releaseScheduleUiState = releaseScheduleUiState,
            )
        },
        onEditActivationDateClick = {
            onEditActivationDateClick(setupOrReviewParam.activationDate)
        },
        onEditNoteClick = {
            onEditNoteClick(setupOrReviewParam.note)
        },
        onNotifyPrefClick = {
            onNotifyPrefClick(setupOrReviewParam.isNotify, setupOrReviewParam.emails)
        },
        onDiscardChange = onDiscardChange,
        onShareSecretClicked = onShareSecretClicked,
        onActionTopBarClick = onActionTopBarClick,
        onViewClaimingInstruction = onViewClaimingInstruction,
        onEditBufferPeriodClick = onEditBufferPeriodClick,
        onBackUpPasswordInfoClick = onBackUpPasswordInfoClick,
        onEditAssetAllocationClick = onEditAssetAllocationClick,
        onEditReleaseMethodClick = onEditReleaseMethodClick,
        onEditBeneficiarySchedulesClick = onEditBeneficiarySchedulesClick,
        onEditFallbackSettingsClick = onEditFallbackSettingsClick,
    )
}

@Composable
fun InheritanceReviewPlanScreenContent(
    isMiniscriptWallet: Boolean = false,
    userEmail: String = "",
    remainTime: Int = 0,
    planFlow: Int = InheritancePlanFlow.VIEW,
    magicalPhrase: String = "",
    groupId: String = "",
    releaseScheduleUiState: ReleaseScheduleUiState = ReleaseScheduleUiState(stages = emptyList()),
    state: InheritanceReviewPlanState = InheritanceReviewPlanState(),
    setupOrReviewParam: InheritancePlanningParam.SetupOrReview = InheritancePlanningParam.SetupOrReview(
        walletId = ""
    ),
    isContinueButtonEnabled: Boolean = true,
    onContinueClicked: () -> Unit = {},
    onShareSecretClicked: () -> Unit = {},
    onDiscardChange: () -> Unit = {},
    onEditActivationDateClick: () -> Unit = {},
    onEditNoteClick: () -> Unit = {},
    onNotifyPrefClick: () -> Unit = {},
    onActionTopBarClick: () -> Unit = {},
    onViewClaimingInstruction: () -> Unit = {},
    onEditBufferPeriodClick: (bufferPeriod: Period?) -> Unit = {},
    onBackUpPasswordInfoClick: () -> Unit = {},
    onEditAssetAllocationClick: () -> Unit = {},
    onEditReleaseMethodClick: () -> Unit = {},
    onEditBeneficiarySchedulesClick: () -> Unit = {},
    onEditFallbackSettingsClick: () -> Unit = {},
) {
    val isEditable = groupId.isEmpty() || state.currentUserRole.toRole.isMasterOrAdmin
    val isSingleBeneficiaryFlow =
        setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.SINGLE_BENEFICIARY
    val isMultiBeneficiaryFlow =
        setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.MULTI_BENEFICIARY
    val magicalPhraseMask = if (groupId.isNotEmpty() && magicalPhrase.isEmpty()) {
        Utils.maskValue("", isMask = true)
    } else {
        magicalPhrase.ifBlank { stringResource(id = R.string.nc_no_listed) }
    }
    val changeHighlightColor = Color(0xFFCF4018)
    val shouldHighlightChangedSection = planFlow == InheritancePlanFlow.VIEW
    val highlights = state.changeHighlights

    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                val title = if (planFlow == InheritancePlanFlow.SETUP) stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ) else ""
                NcTopAppBar(
                    backgroundColor = colorResource(id = R.color.nc_primary_light_color),
                    title = title,
                    isBack = planFlow != InheritancePlanFlow.VIEW,
                    tintColor = Color.White,
                    actions = {
                        IconButton(onClick = {
                            onActionTopBarClick()
                        }) {
                            var showMoreIcon = false
                            if (planFlow == InheritancePlanFlow.SETUP) {
                                showMoreIcon = false
                            } else {
                                if (groupId.isNotEmpty()) {
                                    if (state.currentUserRole.toRole.isMasterOrAdmin) showMoreIcon =
                                        true
                                } else {
                                    showMoreIcon = true
                                }
                            }
                            if (showMoreIcon && isEditable) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_horizontal),
                                    contentDescription = "More",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    val continueText = if (planFlow == InheritancePlanFlow.SETUP) {
                        stringResource(id = R.string.nc_text_continue)
                    } else {
                        stringResource(id = R.string.nc_save)
                    }
                    if (isEditable) {
                        NcPrimaryDarkButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            enabled = isContinueButtonEnabled,
                            onClick = onContinueClicked
                        ) {
                            Text(text = continueText)
                        }
                        if (planFlow == InheritancePlanFlow.VIEW) {
                            NcOutlineButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp)
                                    .height(48.dp),
                                onClick = onDiscardChange,
                            ) {
                                Text(text = stringResource(R.string.nc_discard_changes))
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                // Header Section with Wallet Info
                item {
                    Column(
                        modifier = Modifier
                            .background(color = colorResource(id = R.color.nc_primary_light_color))
                            .padding(bottom = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_review_your_plan),
                            style = NunchukTheme.typography.heading,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(
                                        color = colorResource(id = R.color.nc_primary_light_color),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 16.dp)
                            ) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_assisted_wallet_intro),
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(60.dp),
                                        contentDescription = null
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1.0f)
                                            .padding(start = 8.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(top = 4.dp),
                                            color = colorResource(id = R.color.nc_white_color),
                                            text = stringResource(id = R.string.nc_wallet_subject_to_inheritance),
                                            style = NunchukTheme.typography.title
                                        )
                                        Text(
                                            modifier = Modifier.padding(top = 4.dp),
                                            color = colorResource(id = R.color.nc_white_color),
                                            text = state.walletName.orEmpty(),
                                            style = NunchukTheme.typography.body
                                        )
                                    }
                                }

                                Text(
                                    text = stringResource(R.string.nc_provider_need_these_info_title),
                                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                                    style = NunchukTheme.typography.title,
                                    color = Color.White
                                )
                                if (isMultiBeneficiaryFlow && setupOrReviewParam.beneficiaryAllocations.isNotEmpty()) {
                                    MultiBeneficiaryMagicPhrasesPlanItem(
                                        beneficiaryAllocations = setupOrReviewParam.beneficiaryAllocations
                                    )
                                } else {
                                    SpecialDetailPlanItem(
                                        iconId = R.drawable.ic_star_light,
                                        title = stringResource(R.string.nc_magical_phrase),
                                        content = magicalPhraseMask,
                                        editable = false
                                    )
                                }
                                if (isMiniscriptWallet) {
                                    setupOrReviewParam.inheritanceKeys.forEachIndexed { index, key ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        SpecialDetailPlanItem(
                                            iconId = R.drawable.ic_key,
                                            title = "Inheritance Key ${index + 1}",
                                            subTitle = "XFP: ${key.uppercase()}",
                                            content = stringResource(R.string.nc_12_or_24_word_inheritance_key_backup),
                                            editable = false
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val isSingleKey = setupOrReviewParam.inheritanceKeys.size == 1
                                    SpecialDetailPlanItem(
                                        iconId = R.drawable.ic_password_light,
                                        title = stringResource(if (isSingleKey) R.string.nc_backup_password else R.string.nc_two_backup_password),
                                        actionText = stringResource(id = R.string.nc_text_info),
                                        content = if (!isSingleKey) stringResource(
                                            id = R.string.nc_backup_passwords_desc
                                        ) else stringResource(id = R.string.nc_backup_password_desc),
                                        editable = true,
                                        onClick = {
                                            onBackUpPasswordInfoClick()
                                        }
                                    )
                                }
                                if (setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.OLD_FLOW) {
                                    Text(
                                        text = "Funds become claimable after:",
                                        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                                        style = NunchukTheme.typography.title,
                                        color = Color.White
                                    )
                                    ActivationDateItem(
                                        activationDate = formatDateTimeInTimezone(
                                            timestamp = setupOrReviewParam.activationDate,
                                            isOnChainTimelock = isMiniscriptWallet
                                        ),
                                        timeZoneId = setupOrReviewParam.selectedZoneId,
                                        editable = isEditable && !isMiniscriptWallet,
                                        isHeightLock = setupOrReviewParam.timelockBased == TimelockBased.HEIGHT_LOCK,
                                        blockHeight = setupOrReviewParam.blockHeight,
                                        onClick = {
                                            onEditActivationDateClick()
                                        }
                                    )
                                }
                                if (isEditable && planFlow == InheritancePlanFlow.VIEW) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    NcOutlineButton(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .height(48.dp),
                                        borderColor = Color.White,
                                        onClick = onShareSecretClicked,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.nc_share_your_secrets),
                                            color = Color.White,
                                            style = NunchukTheme.typography.title
                                        )
                                    }
                                    Text(
                                        text = stringResource(id = R.string.nc_view_claiming_instructions),
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        style = NunchukTheme.typography.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                            .clickable {
                                                onViewClaimingInstruction()
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                if (isSingleBeneficiaryFlow) {
                    item(key = "single_beneficiary_release_schedule") {
                        SingleBeneficiaryReleaseScheduleSection(
                            isEditable = isEditable,
                            releaseScheduleUiState = releaseScheduleUiState,
                            bufferSummaryText = getReviewBufferSummaryText(
                                setupOrReviewParam.sharedScheduleConfig?.bufferPeriod,
                                setupOrReviewParam.sharedScheduleConfig?.bufferPeriodApplyType,
                            ),
                            bufferSummaryColor = if (shouldHighlightChangedSection &&
                                highlights.sharedScheduleChanges.bufferPeriodChanged
                            ) {
                                changeHighlightColor
                            } else {
                                MaterialTheme.colorScheme.textPrimary
                            },
                            timeZoneText = getTimezoneDisplay(setupOrReviewParam.selectedZoneId),
                            timeZoneColor = if (shouldHighlightChangedSection && highlights.timezoneChanged) {
                                changeHighlightColor
                            } else {
                                MaterialTheme.colorScheme.textPrimary
                            },
                            onEditReleaseScheduleClick = {
                                onEditBufferPeriodClick(setupOrReviewParam.bufferPeriod)
                            },
                            onEditTimeZoneClick = onEditActivationDateClick
                        )
                    }
                }

                if (isMultiBeneficiaryFlow) {
                    item(key = "multi_beneficiary_summary") {
                        MultiBeneficiaryReviewSection(
                            isEditable = isEditable,
                            releaseMethodType = setupOrReviewParam.releaseMethodType,
                            beneficiaries = setupOrReviewParam.beneficiaryAllocations,
                            sharedReleaseScheduleUiState = releaseScheduleUiState,
                            individualScheduleCardDataByEmail = setupOrReviewParam.individualScheduleConfigs.mapValues { (_, config) ->
                                InheritanceBeneficiaryScheduleCardData(
                                    releaseScheduleUiState = config.releaseScheduleUiState,
                                    bufferPeriodSummaryText = releaseScheduleBufferPeriodSummaryText(
                                        period = config.bufferPeriod,
                                        applyType = config.bufferPeriodApplyType,
                                    ),
                                )
                            },
                            sharedBufferPeriodSummaryText = releaseScheduleBufferPeriodSummaryText(
                                period = setupOrReviewParam.sharedScheduleConfig?.bufferPeriod,
                                applyType = setupOrReviewParam.sharedScheduleConfig?.bufferPeriodApplyType,
                            ),
                            timezoneText = getTimezoneDisplay(setupOrReviewParam.selectedZoneId),
                            fallbackSummaryText = fallbackSettingsSummaryText(setupOrReviewParam.fallbackSettings),
                            changeHighlights = if (shouldHighlightChangedSection) {
                                highlights
                            } else {
                                InheritanceReviewPlanChangeHighlights()
                            },
                            changedTextColor = changeHighlightColor,
                            onEditAssetAllocationClick = onEditAssetAllocationClick,
                            onEditReleaseMethodClick = onEditReleaseMethodClick,
                            onEditBeneficiarySchedulesClick = onEditBeneficiarySchedulesClick,
                            onEditTimeZoneClick = onEditActivationDateClick,
                            onEditFallbackSettingsClick = onEditFallbackSettingsClick,
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp, end = 16.dp, top = 24.dp
                        )
                    ) {
                        val beneficiaryNotes = setupOrReviewParam.beneficiaryAllocations.map { allocation ->
                            InheritancePlanBeneficiary(
                                email = allocation.email,
                                assetPercentage = allocation.allocationPercent,
                                magic = allocation.magic,
                                note = allocation.note,
                            )
                        }
                        ReviewPlanSectionHeader(
                            title = stringResource(
                                id = if (isMultiBeneficiaryFlow) {
                                    R.string.nc_note_to_beneficiary
                                } else {
                                    R.string.nc_note_to_beneficiary_trustee
                                }
                            ),
                            editable = isEditable,
                            onEditClick = onEditNoteClick,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isMultiBeneficiaryFlow) {
                            BeneficiaryNotesSection(
                                beneficiaries = beneficiaryNotes,
                                globalNote = setupOrReviewParam.note,
                                forcePerBeneficiaryNotes = true,
                                itemSpacing = 12.dp,
                                changedEmailKeys = if (shouldHighlightChangedSection) {
                                    highlights.noteChangedEmails
                                } else {
                                    emptySet()
                                },
                                globalNoteChanged = shouldHighlightChangedSection && highlights.globalNoteChanged,
                                textColor = { isChanged ->
                                    if (shouldHighlightChangedSection && isChanged) {
                                        changeHighlightColor
                                    } else {
                                        MaterialTheme.colorScheme.textPrimary
                                    }
                                }
                            )
                        } else {
                            NoteDisplayBox(
                                note = setupOrReviewParam.note,
                                textColor = if (shouldHighlightChangedSection && highlights.globalNoteChanged) {
                                    changeHighlightColor
                                } else {
                                    MaterialTheme.colorScheme.textPrimary
                                },
                            )
                        }
                    }
                }

                if (!isMiniscriptWallet && setupOrReviewParam.setupFlowType == InheritanceSetupFlowType.OLD_FLOW) {
                    item(key = "divider_1") {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.whisper
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp, top = 24.dp
                            )
                        ) {
                            ReviewPlanSectionHeader(
                                title = stringResource(id = R.string.nc_buffer_period),
                                editable = isEditable,
                                onEditClick = { onEditBufferPeriodClick(setupOrReviewParam.bufferPeriod) },
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            NoteDisplayBox(
                                note = setupOrReviewParam.bufferPeriod?.displayName.orEmpty()
                                    .ifBlank { stringResource(id = R.string.nc_no_buffer) }
                            )
                        }
                    }
                }

                // Divider
                item(key = "divider_2") {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.whisper
                    )
                }

                // Notification Preferences Header
                item(key = "notification_preferences_header") {
                    ReviewPlanSectionHeader(
                        modifier = Modifier.padding(
                            start = 16.dp, end = 16.dp, top = 24.dp
                        ),
                        title = stringResource(id = R.string.nc_notification_preferences),
                        editable = isEditable,
                        onEditClick = onNotifyPrefClick,
                    )
                }

                // Notification Settings
                item {
                    NotificationPreferencesSection(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        isMiniscriptWallet = isMiniscriptWallet,
                        notificationPreferences = setupOrReviewParam.notificationSettings,
                        userEmail = userEmail,
                        emails = setupOrReviewParam.emails,
                        isNotifyToday = setupOrReviewParam.isNotify,
                        textColor = if (shouldHighlightChangedSection && highlights.notificationPreferencesChanged) {
                            changeHighlightColor
                        } else {
                            MaterialTheme.colorScheme.textPrimary
                        },
                        emailTextColor = if (shouldHighlightChangedSection && highlights.notificationPreferencesChanged) {
                            changeHighlightColor
                        } else {
                            MaterialTheme.colorScheme.textPrimary
                        },
                        notifyTextColor = if (shouldHighlightChangedSection && highlights.notificationPreferencesChanged) {
                            changeHighlightColor
                        } else {
                            MaterialTheme.colorScheme.textPrimary
                        },
                    )
                }

                // Bottom spacing
                item(key = "notification_bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SingleBeneficiaryReleaseScheduleSection(
    isEditable: Boolean,
    releaseScheduleUiState: ReleaseScheduleUiState,
    bufferSummaryText: String,
    bufferSummaryColor: Color,
    timeZoneText: String,
    timeZoneColor: Color,
    onEditReleaseScheduleClick: () -> Unit,
    onEditTimeZoneClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.fillDenim)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.greyLight,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = com.nunchuk.android.main.R.string.nc_release_schedule_title),
                        style = NunchukTheme.typography.title
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isEditable) {
                        Text(
                            text = stringResource(id = R.string.nc_edit),
                            style = NunchukTheme.typography.title,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable(onClick = onEditReleaseScheduleClick)
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = bufferSummaryText,
                    style = NunchukTheme.typography.titleSmall,
                    color = bufferSummaryColor
                )
                ReleaseScheduleSummaryProgress(
                    modifier = Modifier.padding(top = 12.dp),
                    segments = releaseScheduleUiState.allocationSegments,
                    summaryScalePercent = releaseScheduleUiState.summaryScalePercent,
                    remainingSummaryPercent = releaseScheduleUiState.remainingSummaryPercent,
                    surfaceColor = MaterialTheme.colorScheme.greyLight
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .background(
                    color = MaterialTheme.colorScheme.greyLight,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = com.nunchuk.android.core.R.string.nc_time_zone),
                        style = NunchukTheme.typography.title
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isEditable) {
                        Text(
                            text = stringResource(id = R.string.nc_edit),
                            style = NunchukTheme.typography.title,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable(onClick = onEditTimeZoneClick)
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = timeZoneText,
                    style = NunchukTheme.typography.body,
                    color = timeZoneColor
                )
            }
        }
    }
}

@Composable
private fun MultiBeneficiaryMagicPhrasesPlanItem(
    beneficiaryAllocations: List<InheritanceBeneficiaryAllocation>,
) {
    Column(
        modifier = Modifier
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.ic_nc_star_dark),
                tint = colorResource(id = R.color.nc_grey_g7),
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(id = R.string.nc_magic_phrases),
                color = colorResource(id = R.color.nc_grey_g7),
                style = NunchukTheme.typography.title
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, top = 8.dp)
        ) {
            beneficiaryAllocations.forEachIndexed { index, allocation ->
                Text(
                    text = allocation.email,
                    style = NunchukTheme.typography.title
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = allocation.magic.ifBlank { stringResource(id = R.string.nc_no_listed) },
                    style = NunchukTheme.typography.body
                )
                if (index != beneficiaryAllocations.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.whisper
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiBeneficiaryReviewSection(
    isEditable: Boolean,
    releaseMethodType: InheritanceReleaseMethodType,
    beneficiaries: List<InheritanceBeneficiaryAllocation>,
    sharedReleaseScheduleUiState: ReleaseScheduleUiState,
    individualScheduleCardDataByEmail: Map<String, InheritanceBeneficiaryScheduleCardData>,
    sharedBufferPeriodSummaryText: String?,
    timezoneText: String,
    fallbackSummaryText: String?,
    changeHighlights: InheritanceReviewPlanChangeHighlights,
    changedTextColor: Color,
    onEditAssetAllocationClick: () -> Unit,
    onEditReleaseMethodClick: () -> Unit,
    onEditBeneficiarySchedulesClick: () -> Unit,
    onEditTimeZoneClick: () -> Unit,
    onEditFallbackSettingsClick: () -> Unit,
) {
    val defaultPrimaryTextColor = MaterialTheme.colorScheme.textPrimary
    val defaultSecondaryTextColor = MaterialTheme.colorScheme.textSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.fillDenim)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MultiBeneficiaryCard {
            ReviewPlanSectionHeader(
                title = stringResource(id = R.string.nc_asset_allocation),
                editable = isEditable,
                onEditClick = onEditAssetAllocationClick
            )
            if (beneficiaries.isNotEmpty()) {
                AllocationDonutChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    beneficiaries = beneficiaries,
                    labelPrimaryColorForBeneficiary = { beneficiary ->
                        if (changeHighlights.isAssetAllocationChangedForEmail(beneficiary.email)) {
                            changedTextColor
                        } else {
                            defaultPrimaryTextColor
                        }
                    },
                    labelSecondaryColorForBeneficiary = { beneficiary ->
                        if (changeHighlights.isAssetAllocationChangedForEmail(beneficiary.email)) {
                            changedTextColor
                        } else {
                            defaultSecondaryTextColor
                        }
                    },
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.whisper
            )
            ReviewPlanSectionHeader(
                modifier = Modifier.padding(top = 12.dp),
                title = stringResource(id = R.string.nc_release_method_title),
                editable = isEditable,
                onEditClick = onEditReleaseMethodClick
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(
                    id = if (releaseMethodType == InheritanceReleaseMethodType.SHARED_SCHEDULE) {
                        R.string.nc_release_method_shared_schedule
                    } else {
                        R.string.nc_release_method_individual_schedules
                    }
                ),
                style = NunchukTheme.typography.body,
                color = if (changeHighlights.releaseMethodChanged) {
                    changedTextColor
                } else {
                    defaultPrimaryTextColor
                }
            )
        }

        MultiBeneficiaryCard {
            ReviewPlanSectionHeader(
                title = stringResource(id = R.string.nc_beneficiary_schedules_title),
                editable = isEditable,
                onEditClick = onEditBeneficiarySchedulesClick
            )
            if (releaseMethodType == InheritanceReleaseMethodType.SHARED_SCHEDULE) {
                SharedScheduleReviewSummary(
                    modifier = Modifier.padding(top = 12.dp),
                    uiState = sharedReleaseScheduleUiState,
                    bufferPeriodSummaryText = sharedBufferPeriodSummaryText,
                    scheduleHighlights = changeHighlights.sharedScheduleChanges,
                    changedTextColor = changedTextColor,
                )
            } else {
                IndividualScheduleReviewSummary(
                    modifier = Modifier.padding(top = 12.dp),
                    beneficiaries = beneficiaries,
                    individualScheduleCardDataByEmail = individualScheduleCardDataByEmail,
                    scheduleHighlightsByEmail = changeHighlights.individualScheduleChangesByEmail,
                    changedTextColor = changedTextColor,
                )
            }
        }

        MultiBeneficiaryCard {
            ReviewPlanSectionHeader(
                title = stringResource(id = com.nunchuk.android.core.R.string.nc_time_zone),
                editable = isEditable,
                onEditClick = onEditTimeZoneClick
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = timezoneText,
                style = NunchukTheme.typography.body,
                color = if (changeHighlights.timezoneChanged) {
                    changedTextColor
                } else {
                    defaultPrimaryTextColor
                }
            )
        }

        MultiBeneficiaryCard {
            ReviewPlanSectionHeader(
                title = stringResource(id = R.string.nc_fallback_settings_title),
                editable = isEditable,
                onEditClick = onEditFallbackSettingsClick
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = fallbackSummaryText ?: stringResource(id = R.string.nc_fallback_summary_no_fallback),
                style = NunchukTheme.typography.body,
                color = if (changeHighlights.fallbackSettingsChanged) {
                    changedTextColor
                } else {
                    defaultPrimaryTextColor
                }
            )
        }
    }
}

@Composable
private fun MultiBeneficiaryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.greyLight,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SharedScheduleReviewSummary(
    modifier: Modifier = Modifier,
    uiState: ReleaseScheduleUiState,
    bufferPeriodSummaryText: String?,
    scheduleHighlights: ScheduleChangeHighlights,
    changedTextColor: Color,
) {
    val firstStage = uiState.stages.firstOrNull()
    val defaultPrimaryTextColor = MaterialTheme.colorScheme.textPrimary
    val defaultSecondaryTextColor = MaterialTheme.colorScheme.textSecondary
    Column(modifier = modifier.fillMaxWidth()) {
        if (firstStage != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = WidgetR.drawable.ic_calendar_blank),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textPrimary
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(
                        id = R.string.nc_release_schedule_first_withdrawal,
                        firstStage.firstWithdrawalDate.display()
                    ),
                    style = NunchukTheme.typography.body,
                    color = if (scheduleHighlights.firstWithdrawalChanged) {
                        changedTextColor
                    } else {
                        defaultPrimaryTextColor
                    }
                )
            }
            if (!bufferPeriodSummaryText.isNullOrBlank()) {
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
                        style = NunchukTheme.typography.body,
                        color = if (scheduleHighlights.bufferPeriodChanged) {
                            changedTextColor
                        } else {
                            defaultPrimaryTextColor
                        }
                    )
                }
            }
            ReleaseScheduleSummaryProgress(
                modifier = Modifier.padding(top = 12.dp),
                segments = uiState.allocationSegments,
                summaryScalePercent = uiState.summaryScalePercent,
                remainingSummaryPercent = uiState.remainingSummaryPercent,
                surfaceColor = MaterialTheme.colorScheme.greyLight,
                labelColorForSegment = { segment ->
                    if (scheduleHighlights.changedStageLabelNumbers.contains(segment.stageNumber)) {
                        changedTextColor
                    } else {
                        defaultPrimaryTextColor
                    }
                },
                dateColorForSegment = { segment ->
                    if (scheduleHighlights.changedStageDateNumbers.contains(segment.stageNumber)) {
                        changedTextColor
                    } else {
                        defaultSecondaryTextColor
                    }
                },
            )
        } else {
            Text(
                text = stringResource(id = R.string.nc_beneficiary_schedules_not_set_up_yet),
                style = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.textSecondary)
            )
        }
    }
}

@Composable
private fun IndividualScheduleReviewSummary(
    modifier: Modifier = Modifier,
    beneficiaries: List<InheritanceBeneficiaryAllocation>,
    individualScheduleCardDataByEmail: Map<String, InheritanceBeneficiaryScheduleCardData>,
    scheduleHighlightsByEmail: Map<String, ScheduleChangeHighlights>,
    changedTextColor: Color,
) {
    val defaultPrimaryTextColor = MaterialTheme.colorScheme.textPrimary
    val defaultSecondaryTextColor = MaterialTheme.colorScheme.textSecondary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        beneficiaries.forEach { beneficiary ->
            val beneficiaryKey = beneficiary.email.toEmailKey()
            val scheduleCardData = individualScheduleCardDataByEmail[beneficiary.email]
                ?: individualScheduleCardDataByEmail[beneficiaryKey]
            val firstStage = scheduleCardData?.releaseScheduleUiState?.stages?.firstOrNull()
            val scheduleHighlights = scheduleHighlightsByEmail[beneficiaryKey] ?: ScheduleChangeHighlights()
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = beneficiary.email,
                    style = NunchukTheme.typography.title,
                )
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
                        text = if (firstStage != null) {
                            stringResource(
                                id = R.string.nc_release_schedule_first_withdrawal,
                                firstStage.firstWithdrawalDate.display()
                            )
                        } else {
                            stringResource(id = R.string.nc_beneficiary_schedules_not_set_up_yet)
                        },
                        style = NunchukTheme.typography.body.copy(
                            color = if (firstStage == null) {
                                defaultSecondaryTextColor
                            } else if (scheduleHighlights.firstWithdrawalChanged) {
                                changedTextColor
                            } else {
                                defaultPrimaryTextColor
                            }
                        )
                    )
                }

                if (firstStage != null && !scheduleCardData?.bufferPeriodSummaryText.isNullOrBlank()) {
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
                            text = scheduleCardData?.bufferPeriodSummaryText.orEmpty(),
                            style = NunchukTheme.typography.body,
                            color = if (scheduleHighlights.bufferPeriodChanged) {
                                changedTextColor
                            } else {
                                defaultPrimaryTextColor
                            }
                        )
                    }
                }

                if (firstStage != null) {
                    ReleaseScheduleSummaryProgress(
                        modifier = Modifier.padding(top = 12.dp),
                        segments = scheduleCardData.releaseScheduleUiState.allocationSegments,
                        summaryScalePercent = scheduleCardData.releaseScheduleUiState.summaryScalePercent,
                        remainingSummaryPercent = scheduleCardData.releaseScheduleUiState.remainingSummaryPercent,
                        surfaceColor = MaterialTheme.colorScheme.greyLight,
                        labelColorForSegment = { segment ->
                            if (scheduleHighlights.changedStageLabelNumbers.contains(segment.stageNumber)) {
                                changedTextColor
                            } else {
                                defaultPrimaryTextColor
                            }
                        },
                        dateColorForSegment = { segment ->
                            if (scheduleHighlights.changedStageDateNumbers.contains(segment.stageNumber)) {
                                changedTextColor
                            } else {
                                defaultSecondaryTextColor
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun getReviewBufferSummaryText(
    period: Period?,
    applyType: InheritanceBufferPeriodApplyType?,
): String {
    if (period == null) {
        return stringResource(id = R.string.nc_release_schedule_buffer_period_summary_no_buffer)
    }
    if (applyType == null) return period.displayName
    val applyTypeText = when (applyType) {
        InheritanceBufferPeriodApplyType.FIRST_WITHDRAWAL_ONLY ->
            stringResource(id = com.nunchuk.android.main.R.string.nc_release_schedule_buffer_period_first_withdrawal_only)

        InheritanceBufferPeriodApplyType.EVERY_WITHDRAWAL ->
            stringResource(id = com.nunchuk.android.main.R.string.nc_release_schedule_buffer_period_every_withdrawal)
    }
    return stringResource(
        id = com.nunchuk.android.main.R.string.nc_release_schedule_buffer_period_summary,
        period.intervalCount,
        applyTypeText
    )
}


@Composable
fun SpecialDetailPlanItem(
    title: String,
    subTitle: String = "",
    iconId: Int = R.drawable.ic_nc_star_dark,
    content: String = "dolphin concert apple",
    editable: Boolean = false,
    actionText: String = stringResource(id = R.string.nc_edit),
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .background(
                color = Color.White, shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.Center) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = iconId),
                tint = colorResource(id = R.color.nc_grey_g7),
                contentDescription = ""
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = title,
                    color = colorResource(id = R.color.nc_grey_g7),
                    style = NunchukTheme.typography.title
                )

                if (subTitle.isNotEmpty()) {
                    Text(
                        text = "($subTitle)",
                        color = colorResource(id = R.color.nc_grey_g7),
                        style = NunchukTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.weight(weight = 1f))
            if (editable) {
                Text(
                    modifier = Modifier.clickable {
                        onClick()
                    },
                    text = actionText,
                    color = colorResource(id = R.color.nc_grey_g7),
                    style = NunchukTheme.typography.title,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = content,
            style = NunchukTheme.typography.body,
            color = colorResource(id = R.color.nc_grey_g7),
            modifier = Modifier
                .padding(start = 32.dp)
                .fillMaxWidth()
        )
    }
}

@PreviewLightDark
@Composable
private fun DetailPlanItemPreview() {
    SpecialDetailPlanItem(
        title = "Magical Phrase",
        iconId = R.drawable.ic_star_light,
        content = "dolphin concert apple",
        editable = true,
        onClick = {}
    )
}

@PreviewLightDark
@Composable
private fun UserNotificationSettingsPreview() {
    NunchukTheme {
        UserNotificationSettingsContent(
            emailMeWalletConfig = true,
            userEmail = "user@example.com"
        )
    }
}

@PreviewLightDark
@Composable
private fun ProviderNotificationSettingsPreview() {
    NunchukTheme {
        ProviderNotificationSettingsContent(
            emailSettings = EmailNotificationSettings(
                email = "beneficiary@example.com",
                notifyOnTimelockExpiry = true,
                notifyOnWalletChanges = false,
                includeWalletConfiguration = true
            )
        )
    }
}

@PreviewLightDark
@Composable
private fun SimpleNotificationCardPreview() {
    NunchukTheme {
        SimpleNotificationCard(
            modifier = Modifier.padding(16.dp),
            emails = listOf("email1@example.com", "email2@example.com"),
            isNotifyToday = true,
        )
    }
}

@PreviewLightDark
@Composable
private fun InheritanceReviewPlanScreenPreview() {
    InheritanceReviewPlanScreenContent(
        state = InheritanceReviewPlanState(
            walletName = "My Wallet"
        ),
        setupOrReviewParam = InheritancePlanningParam.SetupOrReview(
            walletId = "wallet123",
            activationDate = System.currentTimeMillis(),
            note = "Sample note",
            emails = listOf("email1@example.com", "email2@example.com"),
            isNotify = true,
            magicalPhrase = "sample magical phrase"
        ),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceReviewPlanSingleBeneficiaryPreview() {
    val stages = listOf(
        ReleaseScheduleStage(
            id = 1,
            stageNumber = 1,
            allocationPercent = 50,
            firstWithdrawalDate = ReleaseScheduleDate(month = 6, day = 15, year = 2027),
            installmentConfig = ReleaseInstallmentConfig(
                installmentPercent = 25,
                repeatEvery = 1,
                frequency = ReleaseInstallmentFrequency.ANNUALLY,
            ),
        ),
        ReleaseScheduleStage(
            id = 2,
            stageNumber = 2,
            allocationPercent = 50,
            firstWithdrawalDate = ReleaseScheduleDate(month = 6, day = 15, year = 2029),
            installmentConfig = ReleaseInstallmentConfig(
                installmentPercent = 50,
                repeatEvery = 6,
                frequency = ReleaseInstallmentFrequency.MONTHLY,
            ),
        ),
    )
    InheritanceReviewPlanScreenContent(
        state = InheritanceReviewPlanState(
            walletName = "Iron Hand Multisig"
        ),
        setupOrReviewParam = InheritancePlanningParam.SetupOrReview(
            walletId = "wallet456",
            activationDate = System.currentTimeMillis(),
            note = "Please contact lawyer John Doe at (555) 123-4567 before claiming.",
            emails = listOf("beneficiary@example.com"),
            isNotify = true,
            magicalPhrase = "dolphin concert apple orange mountain",
            setupFlowType = InheritanceSetupFlowType.SINGLE_BENEFICIARY,
            bufferPeriod = Period(
                id = "30_days",
                interval = "DAYS",
                intervalCount = 30,
                enabled = true,
                displayName = "30 days",
                isRecommended = true,
            ),
            bufferPeriodApplyType = InheritanceBufferPeriodApplyType.FIRST_WITHDRAWAL_ONLY,
            selectedZoneId = "America/New_York",
        ),
        releaseScheduleUiState = ReleaseScheduleUiState(stages = stages),
        planFlow = InheritancePlanFlow.VIEW,
    )
}
