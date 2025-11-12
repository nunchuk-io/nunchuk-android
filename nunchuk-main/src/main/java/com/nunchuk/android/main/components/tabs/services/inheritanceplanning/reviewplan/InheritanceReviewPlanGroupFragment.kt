package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningParam
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningState
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.simpleGlobalDateFormat
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class InheritanceReviewPlanGroupFragment : MembershipFragment(), BottomSheetOptionListener {

    private val viewModel: InheritanceReviewPlanGroupViewModel by viewModels()
    private val inheritanceViewModel: InheritancePlanningViewModel by activityViewModels()
    private val groupId by lazy { inheritanceViewModel.state.value.groupId }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceReviewPlanGroupScreen(
                    viewModel = viewModel,
                    sharedViewModel = inheritanceViewModel,
                    groupId = groupId
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(inheritanceViewModel.setupOrReviewParam)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceReviewPlanGroupEvent.OnContinue -> {
                    requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra(GlobalResultKey.DUMMY_TX_ID, event.dummyTransactionId)
                        putExtra(
                            GlobalResultKey.REQUIRED_SIGNATURES,
                            event.requiredSignatures.requiredSignatures
                        )
                    })
                    requireActivity().finish()
                }

                is InheritanceReviewPlanGroupEvent.Loading -> showOrHideLoading(loading = event.loading)
                is InheritanceReviewPlanGroupEvent.ProcessFailure -> showError(message = event.message)
                InheritanceReviewPlanGroupEvent.CancelInheritanceSuccess -> {}
                InheritanceReviewPlanGroupEvent.CreateOrUpdateInheritanceSuccess -> {}
            }
        }
    }
}

@Composable
fun InheritanceReviewPlanGroupScreen(
    viewModel: InheritanceReviewPlanGroupViewModel = viewModel(),
    sharedViewModel: InheritancePlanningViewModel = viewModel(),
    groupId: String,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedUiState by sharedViewModel.state.collectAsStateWithLifecycle()
    InheritanceReviewPlanGroupScreenContent(
        groupId = groupId,
        sharedUiState = sharedUiState,
        uiState = state,
        onContinueClicked = viewModel::onContinueClick,
    )
}


@Composable
fun InheritanceReviewPlanGroupScreenContent(
    groupId: String = "",
    uiState: InheritanceReviewPlanGroupState = InheritanceReviewPlanGroupState(),
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
            val message = if (groupId.isNotEmpty()) {
                stringResource(
                    id = R.string.nc_create_inheritance_plan_group_change_by,
                    requester?.name ?: "Someone",
                    uiState.walletName
                )
            } else {
                ""
            }
            message
        }

        DummyTransactionType.UPDATE_INHERITANCE_PLAN -> {
            val message = if (groupId.isNotEmpty()) {
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
            message
        }

        DummyTransactionType.CANCEL_INHERITANCE_PLAN -> {
            val message = if (groupId.isNotEmpty()) {
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
            message
        }

        else -> ""
    }

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
                item {
                    if (uiState.dummyTransactionId.isNotEmpty() && uiState.walletName.isNotEmpty()) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = title,
                            style = NunchukTheme.typography.heading
                        )
                        if (desc.isNotEmpty()) {
                            Text(
                                text = desc,
                                style = NunchukTheme.typography.body,
                                modifier = Modifier.padding(
                                    top = 16.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                )
                            )
                        }
                        if (uiState.type == DummyTransactionType.CANCEL_INHERITANCE_PLAN) return@item
                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp, top = 24.dp
                            )
                        ) {
                            // Show "On-chain timelock" for miniscript wallets, "Off-chain timelock" for others
                            val timelockLabel = if (uiState.isMiniscriptWallet) {
                                stringResource(id = R.string.nc_on_chain_timelock)
                            } else {
                                stringResource(id = R.string.nc_off_chain_timelock)
                            }
                            Text(
                                text = timelockLabel,
                                style = NunchukTheme.typography.title
                            )

                            // For miniscript wallets: use SetupOrReview.activationDate and selectedZoneId
                            // For off-chain wallets: use newData.activationTimeMilis with device default timezone
                            val timestamp = if (uiState.isMiniscriptWallet) {
                                sharedUiState.setupOrReviewParam.activationDate
                            } else {
                                newData?.activationTimeMilis.orDefault(0L)
                            }
                            
                            val timeZoneId = if (uiState.isMiniscriptWallet) {
                                sharedUiState.setupOrReviewParam.selectedZoneId
                            } else {
                                "" // Device default timezone
                            }
                            
                            val activationDateTimeText = formatDateTimeInTimezone(
                                timestamp = timestamp,
                                timeZoneId = timeZoneId,
                                isOnChainTimelock = uiState.isMiniscriptWallet
                            )
                            
                            val timezoneDisplayText = getTimezoneDisplay(timeZoneId)
                            
                            // Determine if the date changed (for color highlighting)
                            // On-chain timelock cannot change, so isDateChanged is always false
                            val isDateChanged = if (uiState.isMiniscriptWallet) {
                                false // On-chain timelock cannot change
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
                                        text = activationDateTimeText,
                                        style = NunchukTheme.typography.body.copy(
                                            color = onTextColor(isDateChanged)
                                        ),
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = timezoneDisplayText,
                                        style = NunchukTheme.typography.bodySmall.copy(
                                            color = onTextColor(isDateChanged)
                                        ),
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp, top = 24.dp
                            )
                        ) {
                            Text(
                                text = stringResource(id = R.string.nc_note_to_beneficiary_trustee),
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
                                    text = newData?.note.orEmpty()
                                        .ifBlank { stringResource(id = R.string.nc_no_note) },
                                    style = NunchukTheme.typography.body.copy(
                                        color = onTextColor(
                                            newData?.note != oldData?.note
                                        )
                                    ),
                                )
                            }
                        }

                        if (!uiState.isMiniscriptWallet) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 16.dp, end = 16.dp, top = 24.dp
                                )
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

                        // Notification Preferences Section
                        val newNotificationPreferences = newData?.notificationPreferences
                        if (uiState.isMiniscriptWallet && newNotificationPreferences != null) {
                            Column {
                                Text(
                                    modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp, top = 24.dp
                                    ),
                                    text = stringResource(id = R.string.nc_notification_preferences),
                                    style = NunchukTheme.typography.title,
                                )

                                // User Notification Settings (Owner Email)
                                if (uiState.userEmail.isNotEmpty()) {
                                    UserNotificationSettingsContent(
                                        emailMeWalletConfig = newNotificationPreferences.emailMeWalletConfig,
                                        userEmail = uiState.userEmail,
                                        textColor = onTextColor(
                                            !notificationPreferencesEqual(
                                                newNotificationPreferences,
                                                oldData?.notificationPreferences
                                            )
                                        )
                                    )
                                }

                                // Provider Notification Settings (Beneficiary/Trustee Emails)
                                newNotificationPreferences.perEmailSettings.forEach { emailSettings ->
                                    ProviderNotificationSettingsContent(
                                        emailSettings = emailSettings,
                                        textColor = onTextColor(
                                            !notificationPreferencesEqual(
                                                newNotificationPreferences,
                                                oldData?.notificationPreferences
                                            )
                                        )
                                    )
                                }
                            }
                        } else {
                            // Fallback to old format
                            Column(
                                modifier = Modifier.padding(
                                    start = 16.dp, end = 16.dp, top = 24.dp
                                )
                            ) {
                                Text(
                                    text = stringResource(id = R.string.nc_notification_preferences),
                                    style = NunchukTheme.typography.title,
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.greyLight,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stringResource(id = R.string.nc_beneficiary_trustee_email_address),
                                                style = NunchukTheme.typography.body,
                                                modifier = Modifier.fillMaxWidth(0.3f),
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = newData?.notificationEmails.orEmpty()
                                                    .joinToString("\n")
                                                    .ifEmpty { "(${stringResource(id = R.string.nc_none)})" },
                                                style = NunchukTheme.typography.title.copy(
                                                    color = onTextColor(
                                                        newData?.notificationEmails != oldData?.notificationEmails
                                                    )
                                                )
                                            )
                                        }

                                        HorizontalDivider(
                                            modifier = Modifier.padding(
                                                start = 16.dp,
                                                end = 16.dp,
                                                top = 24.dp,
                                                bottom = 24.dp
                                            ),
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.whisper
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stringResource(id = R.string.nc_notify_them_today),
                                                style = NunchukTheme.typography.body,
                                                modifier = Modifier.fillMaxWidth(0.3f),
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = if (newData?.notifyToday.orFalse()) stringResource(
                                                    id = R.string.nc_text_yes
                                                ) else stringResource(
                                                    id = R.string.nc_text_no
                                                ), style = NunchukTheme.typography.title.copy(
                                                    color = onTextColor(
                                                        newData?.notifyToday != oldData?.notifyToday
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun notificationPreferencesEqual(
    new: InheritanceNotificationSettings?,
    old: InheritanceNotificationSettings?
): Boolean {
    if (new == null && old == null) return true
    if (new == null || old == null) return false
    if (new.emailMeWalletConfig != old.emailMeWalletConfig) return false
    if (new.perEmailSettings.size != old.perEmailSettings.size) return false
    return new.perEmailSettings.all { newSettings ->
        old.perEmailSettings.any { oldSettings ->
            oldSettings.email == newSettings.email &&
                    oldSettings.notifyOnTimelockExpiry == newSettings.notifyOnTimelockExpiry &&
                    oldSettings.notifyOnWalletChanges == newSettings.notifyOnWalletChanges &&
                    oldSettings.includeWalletConfiguration == newSettings.includeWalletConfiguration
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceReviewPlanGroupScreenPreview() {
    InheritanceReviewPlanGroupScreenContent(
        sharedUiState = InheritancePlanningState(
            setupOrReviewParam = InheritancePlanningParam.SetupOrReview(
                walletId = "wallet123",
                activationDate = System.currentTimeMillis(),
                note = "Sample note",
                emails = listOf("email1@example.com", "email2@example.com"),
                isNotify = true,
                magicalPhrase = "sample magical phrase"
            ),
        )
    )
}



