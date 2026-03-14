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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.nunchuk.android.compose.greyLight
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
import java.util.Date

@Composable
fun InheritanceAlertReviewScreen(
    viewModel: InheritanceAlertReviewViewModel = viewModel(),
    sharedViewModel: InheritancePlanningViewModel = viewModel(),
    groupId: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedUiState by sharedViewModel.state.collectAsStateWithLifecycle()
    InheritanceAlertReviewScreenContent(
        groupId = groupId,
        sharedUiState = sharedUiState,
        uiState = state,
        onContinueClicked = viewModel::onContinueClick,
    )
}


@Composable
fun InheritanceAlertReviewScreenContent(
    groupId: String = "",
    uiState: InheritanceAlertReviewState = InheritanceAlertReviewState(),
    sharedUiState: InheritancePlanningState,
    onContinueClicked: () -> Unit = {},
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
            } else if (!uiState.isMiniscriptWallet) {
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

    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
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

    // Asset allocation (multi-beneficiary only)
    if (isMultiBeneficiary && newData.beneficiaries.isNotEmpty()) {
        item(key = "asset_allocation") {
            ReviewPlanSectionHeader(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                title = stringResource(id = R.string.nc_asset_allocation),
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
            )
        }
    }

    // Release method (multi-beneficiary only)
    if (isMultiBeneficiary && newData.releaseMethod != null) {
        item(key = "release_method") {
            ReviewPlanSectionHeader(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                title = stringResource(id = R.string.nc_release_method_title),
            )
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
            )
        }
    } else if (!isMultiBeneficiary) {
        val stages = newData.stages.ifEmpty {
            newData.beneficiaries.firstOrNull()?.stages.orEmpty()
        }
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
    val fallbackPolicy = newData.fallbackPolicy
    if (fallbackPolicy != null) {
        item(key = "fallback_settings") {
            ReviewPlanSectionHeader(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                title = stringResource(id = R.string.nc_fallback_settings_title),
            )
            NoteDisplayBox(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                note = getFallbackPolicySummary(fallbackPolicy),
                textColor = onTextColor(fallbackPolicy != oldData?.fallbackPolicy),
            )
        }
    }

    // Note to Beneficiary
    item(key = "note_to_beneficiary") {
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
            oldNote = oldData?.note,
            oldBeneficiaries = oldData?.beneficiaries,
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
            notifyTextColor = onTextColor(newData.notifyToday != oldData?.notifyToday),
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
                    notifyTextColor = onTextColor(
                        newData?.notifyToday != oldData?.notifyToday
                    ),
                )
            }
        }
    }
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
