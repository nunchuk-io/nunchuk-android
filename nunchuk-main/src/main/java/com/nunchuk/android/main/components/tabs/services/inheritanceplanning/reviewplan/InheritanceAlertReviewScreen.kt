package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.data.model.byzantine.InheritanceDataExtended
import com.nunchuk.android.core.data.model.byzantine.InheritancePayload
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBeneficiaryAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningParam
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningState
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.view.AllocationDonutChart
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.model.inheritance.InheritancePlanBeneficiary
import com.nunchuk.android.model.inheritance.InheritancePlanFallbackPolicy
import com.nunchuk.android.model.inheritance.InheritancePlanStage
import com.nunchuk.android.utils.simpleGlobalDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

private val CHANGED_TEXT_COLOR = Color(0xffCF4018)

@Composable
fun InheritanceAlertReviewScreen(
    viewModel: InheritanceAlertReviewViewModel = viewModel(),
    sharedViewModel: InheritancePlanningViewModel = viewModel(),
    groupId: String,
    onCancelChangeClicked: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedUiState by sharedViewModel.state.collectAsStateWithLifecycle()
    InheritanceAlertReviewScreenContent(
        groupId = groupId,
        sharedUiState = sharedUiState,
        uiState = state,
        onContinueClicked = viewModel::onContinueClick,
        onCancelChangeClicked = onCancelChangeClicked,
    )
}


@Composable
fun InheritanceAlertReviewScreenContent(
    groupId: String = "",
    uiState: InheritanceAlertReviewState = InheritanceAlertReviewState(),
    sharedUiState: InheritancePlanningState,
    onContinueClicked: () -> Unit = {},
    onCancelChangeClicked: () -> Unit = {},
) {
    val newData = uiState.payload.newData
    val oldData = uiState.payload.oldData

    if (newData == null && oldData == null && uiState.type != DummyTransactionType.CANCEL_INHERITANCE_PLAN) {
        return
    }

    val requester by remember(uiState.members, uiState.requestByUserId) {
        derivedStateOf {
            uiState.members.find { it.userId == uiState.requestByUserId }
        }
    }

    val onTextColor: @Composable (isChanged: Boolean) -> Color = {
        if (oldData != null && it) Color(0xffCF4018) else MaterialTheme.colorScheme.controlFillPrimary
    }

    val title =
        when (uiState.type) {
            DummyTransactionType.CREATE_INHERITANCE_PLAN -> stringResource(
                id = R.string.nc_inheritance_plan_group_create,
                uiState.walletName
            )

            DummyTransactionType.UPDATE_INHERITANCE_PLAN, DummyTransactionType.CANCEL_INHERITANCE_PLAN -> stringResource(
                id = R.string.nc_inheritance_plan_group_change,
                uiState.walletName
            )

            else -> ""
        }

    val desc = when (uiState.type) {
        DummyTransactionType.CREATE_INHERITANCE_PLAN -> {
            if (groupId.isNotEmpty()) {
                stringResource(
                    id = R.string.nc_create_inheritance_plan_group_change_by,
                    requester?.name ?: "Someone",
                    uiState.walletName
                )
            } else {
                ""
            }
        }

        DummyTransactionType.UPDATE_INHERITANCE_PLAN -> {
            if (groupId.isNotEmpty()) {
                stringResource(
                    id = R.string.nc_update_inheritance_plan_group_change_by,
                    requester?.name ?: "Someone",
                    uiState.walletName
                )
            } else if (!uiState.isMiniscriptWallet && oldData?.activationTimeMilis != newData?.activationTimeMilis) {
                stringResource(
                    id = R.string.nc_activation_date_inheritance_plan_normal_assisted,
                    uiState.walletName,
                    Date(oldData?.activationTimeMilis.orDefault(0L)).simpleGlobalDateFormat(),
                    Date(newData?.activationTimeMilis.orDefault(0L)).simpleGlobalDateFormat()
                )
            } else {
                ""
            }
        }

        DummyTransactionType.CANCEL_INHERITANCE_PLAN -> {
            if (groupId.isNotEmpty()) {
                stringResource(
                    id = R.string.nc_cancel_inheritance_plan_group_change_by,
                    requester?.name ?: "Someone",
                    uiState.walletName
                )
            } else {
                stringResource(
                    id = R.string.nc_cancel_inheritance_plan_normal_assisted,
                    uiState.walletName
                )
            }
        }

        else -> ""
    }

    val isNewFlow = newData?.beneficiaryMode != null

    var showCancelDialog by remember { mutableStateOf(false) }

    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                Column {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp), onContinueClicked
                    ) {
                        Text(
                            text = pluralStringResource(
                                id = R.plurals.nc_text_continue_signature_pending,
                                count = uiState.pendingSignatures,
                                uiState.pendingSignatures
                            )
                        )
                    }
                    if (uiState.dummyTransactionId.isNotEmpty()) {
                        TextButton(
                            modifier = Modifier
                                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                                .fillMaxWidth(),
                            onClick = { showCancelDialog = true }
                        ) {
                            Text(
                                text = stringResource(id = R.string.nc_cancel_change),
                                color = MaterialTheme.colorScheme.textPrimary,
                                style = NunchukTheme.typography.title
                            )
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
                // Title & description
                item {
                    if (uiState.dummyTransactionId.isNotEmpty() && uiState.walletName.isNotEmpty()) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = title,
                            style = NunchukTheme.typography.heading
                        )
                        if (desc.isNotEmpty()) {
                            NcSpannedText(
                                text = desc,
                                baseStyle = NunchukTheme.typography.body,
                                modifier = Modifier.padding(
                                    top = 16.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                ),
                                styles = mapOf(
                                    SpanIndicator('B') to SpanStyle(fontWeight = FontWeight.Bold),
                                    SpanIndicator('A') to SpanStyle(fontWeight = FontWeight.Bold),
                                )
                            )
                        }
                    }
                }

                if (uiState.type == DummyTransactionType.CANCEL_INHERITANCE_PLAN) return@LazyColumn

                if (isNewFlow && newData != null) {
                    newFlowItems(
                        newData = newData,
                        oldData = oldData,
                        sharedUiState = sharedUiState,
                        isMiniscriptWallet = uiState.isMiniscriptWallet,
                        userEmail = uiState.userEmail,
                        onTextColor = onTextColor,
                    )
                } else {
                    oldFlowItems(
                        newData = newData,
                        oldData = oldData,
                        sharedUiState = sharedUiState,
                        uiState = uiState,
                        onTextColor = onTextColor,
                    )
                }
            }
        }

        if (showCancelDialog) {
            NcConfirmationDialog(
                title = stringResource(id = com.nunchuk.android.core.R.string.nc_confirmation),
                message = stringResource(id = com.nunchuk.android.core.R.string.nc_are_you_sure_cancel_the_change),
                onPositiveClick = {
                    showCancelDialog = false
                    onCancelChangeClicked()
                },
                onDismiss = { showCancelDialog = false }
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.newFlowItems(
    newData: InheritanceDataExtended,
    oldData: InheritanceDataExtended?,
    sharedUiState: InheritancePlanningState,
    isMiniscriptWallet: Boolean,
    userEmail: String,
    onTextColor: @Composable (isChanged: Boolean) -> Color,
) {
    val isMultiBeneficiary = newData.beneficiaryMode == "MULTIPLE"
    val assetAllocationChangedEmails = if (oldData != null) {
        assetAllocationChangedEmailKeys(newData, oldData)
    } else emptySet()
    val individualScheduleHighlightsByEmail = if (oldData != null) {
        individualScheduleHighlights(newData.beneficiaries, oldData.beneficiaries)
    } else emptyMap()
    val sharedScheduleHighlights = if (oldData != null) {
        scheduleHighlights(
            newStages = newData.stages,
            oldStages = oldData.stages,
            newBufferPeriodId = newData.bufferPeriod?.id,
            oldBufferPeriodId = oldData.bufferPeriod?.id,
            newBufferApplyOn = newData.bufferApplyOn,
            oldBufferApplyOn = oldData.bufferApplyOn,
        )
    } else ScheduleChangeHighlights()

    // Asset allocation & Release method (multi-beneficiary only)
    if (isMultiBeneficiary && newData.beneficiaries.isNotEmpty()) {
        item(key = "asset_allocation_and_release_method") {
            val defaultPrimaryColor = MaterialTheme.colorScheme.textPrimary
            val defaultSecondaryColor = MaterialTheme.colorScheme.textSecondary
            val isAssetAllocationChanged: (String) -> Boolean = { email ->
                assetAllocationChangedEmails.contains(email.toEmailKey())
            }
            ReviewPlanSectionHeader(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                title = stringResource(id = R.string.nc_asset_allocation_and_release_method),
            )
            AllocationDonutChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                beneficiaries = newData.beneficiaries.map {
                    InheritanceBeneficiaryAllocation(
                        email = it.email,
                        allocationPercent = it.assetPercentage,
                    )
                },
                labelPrimaryColorForBeneficiary = { beneficiary ->
                    if (isAssetAllocationChanged(beneficiary.email)) CHANGED_TEXT_COLOR
                    else defaultPrimaryColor
                },
                labelSecondaryColorForBeneficiary = { beneficiary ->
                    if (isAssetAllocationChanged(beneficiary.email)) CHANGED_TEXT_COLOR
                    else defaultSecondaryColor
                },
            )
            if (newData.releaseMethod != null) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
                    text = when (newData.releaseMethod) {
                        "INDIVIDUAL" -> stringResource(id = R.string.nc_release_method_individual_schedules)
                        else -> stringResource(id = R.string.nc_release_method_shared_schedule)
                    },
                    style = NunchukTheme.typography.body.copy(
                        color = onTextColor(newData.releaseMethod != oldData?.releaseMethod)
                    ),
                )
            }
        }
    }

    // Beneficiary schedules
    if (isMultiBeneficiary && newData.releaseMethod == "INDIVIDUAL") {
        item(key = "beneficiary_schedules_header") {
            ReviewPlanSectionHeader(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                title = stringResource(id = R.string.nc_beneficiary_schedules_title),
            )
        }
        items(
            items = newData.beneficiaries,
            key = { "beneficiary_schedule_${it.email}" }
        ) { beneficiary ->
            BeneficiaryScheduleReviewCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.greyLight,
                        shape = RoundedCornerShape(8.dp)
                    ),
                beneficiary = beneficiary,
                scheduleHighlights = individualScheduleHighlightsByEmail[beneficiary.email.toEmailKey()]
                    ?: ScheduleChangeHighlights(),
                changedTextColor = CHANGED_TEXT_COLOR,
            )
        }
    } else if (isMultiBeneficiary && newData.stages.isNotEmpty()) {
        item(key = "beneficiary_schedules_shared") {
            ReviewPlanSectionHeader(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                title = stringResource(id = R.string.nc_beneficiary_schedules_title),
            )
            SharedScheduleReviewCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.greyLight,
                        shape = RoundedCornerShape(8.dp)
                    ),
                stages = newData.stages,
                bufferPeriod = newData.bufferPeriod,
                bufferApplyOn = newData.bufferApplyOn,
                scheduleHighlights = sharedScheduleHighlights,
                changedTextColor = CHANGED_TEXT_COLOR,
            )
        }
    } else if (!isMultiBeneficiary) {
        val stages = newData.stages.ifEmpty {
            newData.beneficiaries.firstOrNull()?.stages.orEmpty()
        }
        val singleHighlights = if (oldData != null) {
            val oldStages = oldData.stages.ifEmpty {
                oldData.beneficiaries.firstOrNull()?.stages.orEmpty()
            }
            scheduleHighlights(
                newStages = stages,
                oldStages = oldStages,
                newBufferPeriodId = newData.bufferPeriod?.id
                    ?: newData.beneficiaries.firstOrNull()?.bufferPeriod?.id,
                oldBufferPeriodId = oldData.bufferPeriod?.id
                    ?: oldData.beneficiaries.firstOrNull()?.bufferPeriod?.id,
                newBufferApplyOn = newData.bufferApplyOn
                    ?: newData.beneficiaries.firstOrNull()?.bufferApplyOn,
                oldBufferApplyOn = oldData.bufferApplyOn
                    ?: oldData.beneficiaries.firstOrNull()?.bufferApplyOn,
            )
        } else ScheduleChangeHighlights()
        if (stages.isNotEmpty()) {
            item(key = "single_beneficiary_schedule") {
                ReviewPlanSectionHeader(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                    title = stringResource(id = R.string.nc_beneficiary_schedules_title),
                )
                SharedScheduleReviewCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.greyLight,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    stages = stages,
                    bufferPeriod = newData.bufferPeriod,
                    bufferApplyOn = newData.bufferApplyOn
                        ?: newData.beneficiaries.firstOrNull()?.bufferApplyOn,
                    scheduleHighlights = singleHighlights,
                    changedTextColor = CHANGED_TEXT_COLOR,
                )
            }
        }
    }

    // Timezone
    item(key = "timezone") {
        val timeZoneId = newData.timezone.ifEmpty {
            sharedUiState.setupOrReviewParam.selectedZoneId
        }
        ReviewPlanSectionHeader(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
            title = stringResource(id = R.string.nc_time_zone),
        )
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .background(
                    color = MaterialTheme.colorScheme.greyLight,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = getTimezoneDisplay(timeZoneId),
                style = NunchukTheme.typography.body.copy(
                    color = onTextColor(newData.timezone != oldData?.timezone)
                ),
            )
        }
    }

    // Fallback settings
    item(key = "fallback_settings") {
        val fallbackPolicy = newData.fallbackPolicy
        val fallbackTimezoneId = newData.timezone.ifEmpty {
            sharedUiState.setupOrReviewParam.selectedZoneId
        }
        val oldFallbackTimezoneId = oldData?.timezone?.ifEmpty {
            sharedUiState.setupOrReviewParam.selectedZoneId
        }.orEmpty()
        val fallbackChanged = fallbackPolicyComparisonKey(fallbackPolicy, fallbackTimezoneId) !=
            fallbackPolicyComparisonKey(oldData?.fallbackPolicy, oldFallbackTimezoneId)
        ReviewPlanSectionHeader(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
            title = stringResource(id = R.string.nc_fallback_settings_title),
        )
        NoteDisplayBox(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            note = getFallbackPolicySummary(fallbackPolicy, fallbackTimezoneId),
            textColor = onTextColor(fallbackChanged),
        )
    }

    // Note to Beneficiary
    item(key = "note_to_beneficiary") {
        val oldNotesByEmail = oldData
            ?.beneficiaries
            ?.associateBy(
                keySelector = { it.email.trim().lowercase() },
                valueTransform = { it.note.orEmpty() },
            )
            .orEmpty()
        val changedNoteEmailKeys = newData.beneficiaries
            .asSequence()
            .mapNotNull { beneficiary ->
                val emailKey = beneficiary.email.trim().lowercase()
                val oldNote = oldNotesByEmail[emailKey].orEmpty()
                if (beneficiary.note.orEmpty() != oldNote) emailKey else null
            }
            .toSet()

        ReviewPlanSectionHeader(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
            title = stringResource(id = R.string.nc_note_to_beneficiary_trustee),
        )
        Spacer(modifier = Modifier.height(12.dp))
        BeneficiaryNotesSection(
            modifier = Modifier.padding(horizontal = 16.dp),
            beneficiaries = newData.beneficiaries,
            globalNote = newData.note,
            textColor = onTextColor,
            changedEmailKeys = changedNoteEmailKeys,
            globalNoteChanged = newData.note.orEmpty() != oldData?.note.orEmpty(),
        )
    }

    // Notification preferences
    item(key = "notification_preferences") {
        val notifPrefs = newData.notificationPreferences
        val notifChanged = !notificationPreferencesEqual(
            notifPrefs, oldData?.notificationPreferences
        )
        ReviewPlanSectionHeader(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
            title = stringResource(id = R.string.nc_notification_preferences),
        )
        NotificationPreferencesSection(
            modifier = Modifier.padding(horizontal = 16.dp),
            isMiniscriptWallet = isMiniscriptWallet,
            notificationPreferences = notifPrefs,
            userEmail = userEmail,
            emails = newData.notificationEmails,
            isNotifyToday = newData.notifyToday,
            textColor = onTextColor(notifChanged),
            emailTextColor = onTextColor(newData.notificationEmails != oldData?.notificationEmails),
            notifyTextColor = onTextColor(notifyTodayChanged(newData, oldData)),
        )
    }

    item(key = "bottom_spacer") {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.oldFlowItems(
    newData: InheritanceDataExtended?,
    oldData: InheritanceDataExtended?,
    sharedUiState: InheritancePlanningState,
    uiState: InheritanceAlertReviewState,
    onTextColor: @Composable (isChanged: Boolean) -> Color,
) {
    item {
        if (uiState.dummyTransactionId.isNotEmpty() && uiState.walletName.isNotEmpty()) {
            // Timelock
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
            ) {
                val timelockLabel = if (uiState.isMiniscriptWallet) {
                    stringResource(id = R.string.nc_on_chain_timelock)
                } else {
                    stringResource(id = R.string.nc_off_chain_timelock)
                }
                Text(text = timelockLabel, style = NunchukTheme.typography.title)

                val timestamp = if (uiState.isMiniscriptWallet) {
                    sharedUiState.setupOrReviewParam.activationDate
                } else {
                    newData?.activationTimeMilis.orDefault(0L)
                }
                val timeZoneId = if (uiState.isMiniscriptWallet) {
                    sharedUiState.setupOrReviewParam.selectedZoneId
                } else {
                    newData?.timezone.orEmpty()
                }
                val isDateChanged = if (uiState.isMiniscriptWallet) {
                    false
                } else {
                    newData?.activationTimeMilis != oldData?.activationTimeMilis
                }

                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.greyLight,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = formatDateTimeInTimezone(
                                timestamp = timestamp,
                                isOnChainTimelock = uiState.isMiniscriptWallet
                            ),
                            style = NunchukTheme.typography.body.copy(
                                color = onTextColor(isDateChanged)
                            ),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getTimezoneDisplay(timeZoneId),
                            style = NunchukTheme.typography.bodySmall.copy(
                                color = onTextColor(isDateChanged)
                            ),
                        )
                    }
                }
            }

            // Note
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.nc_note_to_beneficiary_trustee),
                    style = NunchukTheme.typography.title
                )
                NoteDisplayBox(
                    modifier = Modifier.padding(top = 12.dp),
                    note = newData?.note.orEmpty(),
                    textColor = onTextColor(newData?.note != oldData?.note),
                )
            }

            // Buffer period (non-miniscript only)
            if (!uiState.isMiniscriptWallet) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_buffer_period),
                        style = NunchukTheme.typography.title
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.greyLight,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            text = newData?.bufferPeriod?.displayName.orEmpty()
                                .ifBlank { stringResource(id = R.string.nc_no_buffer) },
                            style = NunchukTheme.typography.body.copy(
                                color = onTextColor(
                                    newData?.bufferPeriod?.id != oldData?.bufferPeriod?.id
                                )
                            ),
                        )
                    }
                }
            }

            // Notification Preferences
            val newNotificationPreferences = newData?.notificationPreferences
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.nc_notification_preferences),
                    style = NunchukTheme.typography.title,
                )
                NotificationPreferencesSection(
                    isMiniscriptWallet = uiState.isMiniscriptWallet,
                    notificationPreferences = newNotificationPreferences,
                    userEmail = uiState.userEmail,
                    emails = newData?.notificationEmails.orEmpty(),
                    isNotifyToday = newData?.notifyToday.orFalse(),
                    textColor = onTextColor(
                        !notificationPreferencesEqual(
                            newNotificationPreferences,
                            oldData?.notificationPreferences
                        )
                    ),
                    emailTextColor = onTextColor(
                        newData?.notificationEmails != oldData?.notificationEmails
                    ),
                    notifyTextColor = onTextColor(notifyTodayChanged(newData, oldData)),
                )
            }
        }
    }
}

// ─── Change-highlight helpers ───────────────────────────────────────────────

/**
 * Builds a time-zone-tolerant identity for a fallback policy so a pure time-zone change is not
 * mistaken for a fallback edit. A DATE_BASED fallback re-parses the same calendar date into a new
 * instant when the time zone changes (shifting [InheritancePlanFallbackPolicy.fallbackTimeMillis]
 * while the displayed date is unchanged), and a NONE/INACTIVITY policy may carry residual fields
 * from the server. Comparing this rendered identity instead of the raw object keeps genuine
 * fallback edits highlighted while ignoring those submit-pipeline artifacts.
 */
private fun fallbackPolicyComparisonKey(
    policy: InheritancePlanFallbackPolicy?,
    timezoneId: String,
): String {
    if (policy == null) return "NONE"
    return when (policy.type.uppercase()) {
        "NONE" -> "NONE"
        "DATE_BASED" -> "DATE_BASED:${policy.fallbackTimeMillis.toFallbackDateDisplay(timezoneId)}"
        else -> "INACTIVITY:${policy.inactivityInterval?.uppercase().orEmpty()}:${policy.inactivityIntervalCount ?: 0}"
    }
}

private fun Long?.toFallbackDateDisplay(timezoneId: String): String {
    if (this == null || this <= 0L) return ""
    return runCatching {
        val zoneId = if (timezoneId.isBlank()) ZoneId.systemDefault() else ZoneId.of(timezoneId)
        Instant.ofEpochMilli(this)
            .atZone(zoneId)
            .format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
    }.getOrDefault("")
}

/**
 * "Also notify them today" has no field in the plan GET response, so on the review path it is
 * derived from whether notification emails exist and re-submitted on every change. A notify-today
 * diff that is not accompanied by an email change is therefore a re-submission artifact (e.g. a
 * pure time-zone change), not a user edit, and must not be highlighted.
 */
private fun notifyTodayChanged(
    newData: InheritanceDataExtended?,
    oldData: InheritanceDataExtended?,
): Boolean {
    return newData?.notifyToday != oldData?.notifyToday &&
        newData?.notificationEmails != oldData?.notificationEmails
}

private fun assetAllocationChangedEmailKeys(
    newData: InheritanceDataExtended,
    oldData: InheritanceDataExtended,
): Set<String> {
    val newAllocations = newData.beneficiaries.associate {
        it.email.toEmailKey() to it.assetPercentage
    }
    val oldAllocations = oldData.beneficiaries.associate {
        it.email.toEmailKey() to it.assetPercentage
    }
    return (newAllocations.keys + oldAllocations.keys)
        .filterTo(mutableSetOf()) { emailKey ->
            newAllocations[emailKey] != oldAllocations[emailKey]
        }
}

private fun individualScheduleHighlights(
    newBeneficiaries: List<InheritancePlanBeneficiary>,
    oldBeneficiaries: List<InheritancePlanBeneficiary>,
): Map<String, ScheduleChangeHighlights> {
    val oldByEmail = oldBeneficiaries.associateBy { it.email.toEmailKey() }
    val newByEmail = newBeneficiaries.associateBy { it.email.toEmailKey() }
    return (newByEmail.keys + oldByEmail.keys).associateWith { emailKey ->
        val new = newByEmail[emailKey]
        val old = oldByEmail[emailKey]
        scheduleHighlights(
            newStages = new?.stages.orEmpty(),
            oldStages = old?.stages.orEmpty(),
            newBufferPeriodId = new?.bufferPeriod?.id ?: new?.bufferPeriodId,
            oldBufferPeriodId = old?.bufferPeriod?.id ?: old?.bufferPeriodId,
            newBufferApplyOn = new?.bufferApplyOn,
            oldBufferApplyOn = old?.bufferApplyOn,
        )
    }
}

private fun scheduleHighlights(
    newStages: List<InheritancePlanStage>,
    oldStages: List<InheritancePlanStage>,
    newBufferPeriodId: String?,
    oldBufferPeriodId: String?,
    newBufferApplyOn: String?,
    oldBufferApplyOn: String?,
): ScheduleChangeHighlights {
    val firstWithdrawalChanged = newStages.firstOrNull()?.firstWithdrawalTimeMillis !=
            oldStages.firstOrNull()?.firstWithdrawalTimeMillis
    val bufferPeriodChanged = newBufferPeriodId != oldBufferPeriodId ||
            newBufferApplyOn != oldBufferApplyOn
    val totalStages = maxOf(newStages.size, oldStages.size)
    val changedStageLabelNumbers = mutableSetOf<Int>()
    val changedStageDateNumbers = mutableSetOf<Int>()
    for (index in 0 until totalStages) {
        val newStage = newStages.getOrNull(index)
        val oldStage = oldStages.getOrNull(index)
        val stageNumber = index + 1
        if (newStage?.totalStageAllocationPercentage != oldStage?.totalStageAllocationPercentage) {
            changedStageLabelNumbers += stageNumber
        }
        if (newStage?.firstWithdrawalTimeMillis != oldStage?.firstWithdrawalTimeMillis) {
            changedStageDateNumbers += stageNumber
        }
    }
    return ScheduleChangeHighlights(
        firstWithdrawalChanged = firstWithdrawalChanged,
        bufferPeriodChanged = bufferPeriodChanged,
        changedStageLabelNumbers = changedStageLabelNumbers,
        changedStageDateNumbers = changedStageDateNumbers,
    )
}

// ─── Previews ───────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun InheritanceAlertReviewScreenPreview() {
    InheritanceAlertReviewScreenContent(
        sharedUiState = InheritancePlanningState(
            setupOrReviewParam = InheritancePlanningParam.SetupOrReview(
                walletId = "wallet123",
                activationDate = System.currentTimeMillis(),
                note = "Sample note",
                emails = listOf("email1@example.com", "email2@example.com"),
                isNotify = true,
                magicalPhrase = "sample magical phrase"
            ),
        ),
        uiState = InheritanceAlertReviewState(
            dummyTransactionId = "tx123",
            walletName = "My Inheritance Wallet",
            type = DummyTransactionType.UPDATE_INHERITANCE_PLAN,
            pendingSignatures = 2,
            isMiniscriptWallet = false,
            userEmail = "jayce@nunchuk.io",
            payload = InheritancePayload(
                newData = InheritanceDataExtended(
                    activationTimeMilis = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000,
                    note = "This is a note to my beneficiaries and trustees.",
                    bufferPeriod = null,
                    notificationEmails = listOf(""),
                    notificationPreferences = InheritanceNotificationSettings(),
                    walletId = "wallet123",
                    notifyToday = false,
                    groupId = "group123"
                ),
                oldData = InheritanceDataExtended(
                    activationTimeMilis = System.currentTimeMillis(),
                    note = "This is a note to my beneficiaries",
                    bufferPeriod = null,
                    notificationEmails = listOf(""),
                    notificationPreferences = InheritanceNotificationSettings(),
                    walletId = "wallet123",
                    notifyToday = false,
                    groupId = "group123"
                )
            )
        ),
        groupId = "group123"
    )
}

@PreviewLightDark
@Composable
private fun InheritanceAlertReviewMultiBeneficiaryPreview() {
    val beneficiaries = listOf(
        InheritancePlanBeneficiary(
            email = "wife@gmail.com",
            assetPercentage = 50,
            magic = "",
            note = "Want a bipartisan way to boost #travel & grow the #economy? Pass the bipartisan travel and tourism bill.",
            bufferApplyOn = "FIRST_WITHDRAWAL",
            stages = listOf(
                InheritancePlanStage(
                    amountPerReleasePercentage = 34,
                    repeatInterval = "ANNUALLY",
                    repeatIntervalCount = 1,
                    totalStageAllocationPercentage = 34,
                    firstWithdrawalTimeMillis = 1843171200000,
                ),
                InheritancePlanStage(
                    amountPerReleasePercentage = 33,
                    repeatInterval = "ANNUALLY",
                    repeatIntervalCount = 1,
                    totalStageAllocationPercentage = 33,
                    firstWithdrawalTimeMillis = 1969171200000,
                ),
                InheritancePlanStage(
                    amountPerReleasePercentage = 33,
                    repeatInterval = "ANNUALLY",
                    repeatIntervalCount = 1,
                    totalStageAllocationPercentage = 33,
                    firstWithdrawalTimeMillis = 2400000000000,
                ),
            ),
        ),
        InheritancePlanBeneficiary(
            email = "son@gmail.com",
            assetPercentage = 25,
            magic = "",
            note = "Want a bipartisan way to boost #travel & grow the #economy? Pass the bipartisan bill.",
            bufferApplyOn = "EVERY_WITHDRAWAL",
            stages = listOf(
                InheritancePlanStage(
                    amountPerReleasePercentage = 50,
                    repeatInterval = "ANNUALLY",
                    repeatIntervalCount = 1,
                    totalStageAllocationPercentage = 50,
                    firstWithdrawalTimeMillis = 1906329600000,
                ),
                InheritancePlanStage(
                    amountPerReleasePercentage = 50,
                    repeatInterval = "ANNUALLY",
                    repeatIntervalCount = 1,
                    totalStageAllocationPercentage = 50,
                    firstWithdrawalTimeMillis = 1969171200000,
                ),
            ),
        ),
        InheritancePlanBeneficiary(
            email = "daughter@gmail.com",
            assetPercentage = 25,
            magic = "",
            note = "Want a bipartisan way to boost #travel & grow the #economy? Pass the bipartisan travel and tourism bill.",
            bufferApplyOn = null,
            stages = listOf(
                InheritancePlanStage(
                    amountPerReleasePercentage = 100,
                    repeatInterval = "ANNUALLY",
                    repeatIntervalCount = 1,
                    totalStageAllocationPercentage = 100,
                    firstWithdrawalTimeMillis = 1906329600000,
                ),
            ),
        ),
    )
    InheritanceAlertReviewScreenContent(
        sharedUiState = InheritancePlanningState(
            setupOrReviewParam = InheritancePlanningParam.SetupOrReview(
                walletId = "wallet456",
                selectedZoneId = "Asia/Jakarta",
            ),
        ),
        uiState = InheritanceAlertReviewState(
            dummyTransactionId = "tx456",
            walletName = "Iron Hand Multisig",
            type = DummyTransactionType.UPDATE_INHERITANCE_PLAN,
            pendingSignatures = 1,
            isMiniscriptWallet = true,
            userEmail = "hugo@nunchuk.io",
            payload = InheritancePayload(
                newData = InheritanceDataExtended(
                    activationTimeMilis = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000,
                    note = "",
                    bufferPeriod = Period(
                        id = "7d",
                        interval = "DAYS",
                        intervalCount = 7,
                        enabled = true,
                        displayName = "7 days",
                        isRecommended = true,
                    ),
                    notificationEmails = listOf("hugo@nunchuk.io"),
                    walletId = "wallet456",
                    notifyToday = true,
                    groupId = "group456",
                    beneficiaryMode = "MULTIPLE",
                    releaseMethod = "INDIVIDUAL",
                    timezone = "Asia/Jakarta",
                    beneficiaries = beneficiaries,
                    fallbackPolicy = InheritancePlanFallbackPolicy(
                        type = "INACTIVITY",
                        inactivityInterval = "year",
                        inactivityIntervalCount = 5,
                    ),
                ),
            )
        ),
        groupId = "group456",
    )
}

@PreviewLightDark
@Composable
private fun InheritanceAlertReviewSingleBeneficiaryPreview() {
    InheritanceAlertReviewScreenContent(
        sharedUiState = InheritancePlanningState(
            setupOrReviewParam = InheritancePlanningParam.SetupOrReview(
                walletId = "wallet789",
                selectedZoneId = "America/New_York",
            ),
        ),
        uiState = InheritanceAlertReviewState(
            dummyTransactionId = "tx789",
            walletName = "My Personal Wallet",
            type = DummyTransactionType.CREATE_INHERITANCE_PLAN,
            pendingSignatures = 2,
            isMiniscriptWallet = true,
            userEmail = "owner@nunchuk.io",
            payload = InheritancePayload(
                newData = InheritanceDataExtended(
                    activationTimeMilis = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000,
                    note = "Contact lawyer Jane Smith before claiming.",
                    bufferPeriod = Period(
                        id = "30d",
                        interval = "DAYS",
                        intervalCount = 30,
                        enabled = true,
                        displayName = "30 days",
                        isRecommended = false,
                    ),
                    notificationEmails = listOf("beneficiary@example.com"),
                    walletId = "wallet789",
                    notifyToday = true,
                    groupId = "group789",
                    beneficiaryMode = "SINGLE",
                    timezone = "America/New_York",
                    beneficiaries = listOf(
                        InheritancePlanBeneficiary(
                            email = "beneficiary@example.com",
                            assetPercentage = 100,
                            magic = "",
                            note = "Contact lawyer Jane Smith before claiming.",
                            bufferApplyOn = "FIRST_WITHDRAWAL",
                        ),
                    ),
                    stages = listOf(
                        InheritancePlanStage(
                            amountPerReleasePercentage = 50,
                            repeatInterval = "ANNUALLY",
                            repeatIntervalCount = 1,
                            totalStageAllocationPercentage = 50,
                            firstWithdrawalTimeMillis = 1906329600000,
                        ),
                        InheritancePlanStage(
                            amountPerReleasePercentage = 50,
                            repeatInterval = "ANNUALLY",
                            repeatIntervalCount = 1,
                            totalStageAllocationPercentage = 50,
                            firstWithdrawalTimeMillis = 1969171200000,
                        ),
                    ),
                    fallbackPolicy = InheritancePlanFallbackPolicy(type = "NONE"),
                ),
            )
        ),
        groupId = "group789",
    )
}
