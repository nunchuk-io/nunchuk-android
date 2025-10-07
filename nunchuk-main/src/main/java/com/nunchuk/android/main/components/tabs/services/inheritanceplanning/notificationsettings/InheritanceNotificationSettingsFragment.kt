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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceNotificationSettingsFragment : MembershipFragment() {

    private val args: InheritanceNotificationSettingsFragmentArgs by navArgs()
    private val inheritanceViewModel: InheritancePlanningViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceNotificationSettingsScreen(
                    membershipStepManager = membershipStepManager,
                    args = args,
                    inheritanceViewModel = inheritanceViewModel,
                    onContinueClick = { notifyOnTimelockExpiry, notifyOnWalletChanges, includeWalletConfiguration, emailMeWalletConfig ->
                        openReviewPlanScreen(
                            notifyOnTimelockExpiry,
                            notifyOnWalletChanges,
                            includeWalletConfiguration,
                            emailMeWalletConfig
                        )
                    }
                )
            }
        }
    }

    private fun openReviewPlanScreen(
        notifyOnTimelockExpiry: Boolean,
        notifyOnWalletChanges: Boolean,
        includeWalletConfiguration: Boolean,
        emailMeWalletConfig: Boolean
    ) {
        inheritanceViewModel.setOrUpdate(
            inheritanceViewModel.setupOrReviewParam.copy(
                notifyOnTimelockExpiry = notifyOnTimelockExpiry,
                notifyOnWalletChanges = notifyOnWalletChanges,
                includeWalletConfiguration = includeWalletConfiguration,
                emailMeWalletConfig = emailMeWalletConfig
            )
        )
        if (args.isUpdateRequest || inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    EXTRA_NOTIFY_ON_TIMELOCK_EXPIRY to notifyOnTimelockExpiry,
                    EXTRA_NOTIFY_ON_WALLET_CHANGES to notifyOnWalletChanges,
                    EXTRA_INCLUDE_WALLET_CONFIGURATION to includeWalletConfiguration,
                    EXTRA_EMAIL_ME_WALLET_CONFIG to emailMeWalletConfig
                )
            )
            findNavController().popBackStack()
        } else {
            findNavController().navigate(
                InheritanceNotificationSettingsFragmentDirections.actionInheritanceNotificationSettingsFragmentToInheritanceReviewPlanFragment()
            )
        }
    }

    companion object {
        const val REQUEST_KEY = "InheritanceNotificationSettingsFragment"
        const val EXTRA_NOTIFY_ON_TIMELOCK_EXPIRY = "EXTRA_NOTIFY_ON_TIMELOCK_EXPIRY"
        const val EXTRA_NOTIFY_ON_WALLET_CHANGES = "EXTRA_NOTIFY_ON_WALLET_CHANGES"
        const val EXTRA_INCLUDE_WALLET_CONFIGURATION = "EXTRA_INCLUDE_WALLET_CONFIGURATION"
        const val EXTRA_EMAIL_ME_WALLET_CONFIG = "EXTRA_EMAIL_ME_WALLET_CONFIG"
    }
}

@Composable
fun InheritanceNotificationSettingsScreen(
    args: InheritanceNotificationSettingsFragmentArgs,
    inheritanceViewModel: InheritancePlanningViewModel,
    onContinueClick: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    membershipStepManager: MembershipStepManager
) {
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    
    // Internal state for notification settings
    var notifyOnTimelockExpiry by remember { 
        mutableStateOf(inheritanceViewModel.setupOrReviewParam.notifyOnTimelockExpiry) 
    }
    var notifyOnWalletChanges by remember { 
        mutableStateOf(inheritanceViewModel.setupOrReviewParam.notifyOnWalletChanges) 
    }
    var includeWalletConfiguration by remember { 
        mutableStateOf(inheritanceViewModel.setupOrReviewParam.includeWalletConfiguration) 
    }
    var emailMeWalletConfig by remember { 
        mutableStateOf(inheritanceViewModel.setupOrReviewParam.emailMeWalletConfig) 
    }

    InheritanceNotificationSettingsScreenContent(
        remainTime = remainTime,
        emails = inheritanceViewModel.setupOrReviewParam.emails,
        planFlow = inheritanceViewModel.setupOrReviewParam.planFlow,
        isUpdateRequest = args.isUpdateRequest,
        notifyOnTimelockExpiry = notifyOnTimelockExpiry,
        notifyOnWalletChanges = notifyOnWalletChanges,
        includeWalletConfiguration = includeWalletConfiguration,
        emailMeWalletConfig = emailMeWalletConfig,
        onNotifyOnTimelockExpiryChange = { notifyOnTimelockExpiry = it },
        onNotifyOnWalletChangesChange = { notifyOnWalletChanges = it },
        onIncludeWalletConfigurationChange = { includeWalletConfiguration = it },
        onEmailMeWalletConfigChange = { emailMeWalletConfig = it },
        onContinueClick = {
            onContinueClick(
                notifyOnTimelockExpiry,
                notifyOnWalletChanges,
                includeWalletConfiguration,
                emailMeWalletConfig
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InheritanceNotificationSettingsScreenContent(
    remainTime: Int = 0,
    emails: List<String> = emptyList(),
    planFlow: Int = InheritancePlanFlow.NONE,
    isUpdateRequest: Boolean = false,
    notifyOnTimelockExpiry: Boolean = true,
    notifyOnWalletChanges: Boolean = true,
    includeWalletConfiguration: Boolean = true,
    emailMeWalletConfig: Boolean = true,
    onNotifyOnTimelockExpiryChange: (Boolean) -> Unit = {},
    onNotifyOnWalletChangesChange: (Boolean) -> Unit = {},
    onIncludeWalletConfigurationChange: (Boolean) -> Unit = {},
    onEmailMeWalletConfigChange: (Boolean) -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val isSetupFlow = planFlow == InheritancePlanFlow.SETUP && isUpdateRequest.not()

    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                val title = if (isSetupFlow) stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ) else ""
                NcTopAppBar(title = title, actions = {
                    Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                })
            },
            bottomBar = {
                val continueBtnText =
                    if (isSetupFlow) stringResource(id = R.string.nc_text_continue) else stringResource(
                        id = R.string.nc_update_notification_preferences
                    )

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClick
                ) {
                    Text(text = continueBtnText)
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_notification_preferences),
                    style = NunchukTheme.typography.heading
                )

                // Email addresses section
                if (emails.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_beneficiary_trustee_email_address),
                        style = NunchukTheme.typography.titleSmall
                    )
                    
                    // Email display box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.backgroundMidGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = emails.joinToString(", "),
                            style = NunchukTheme.typography.body,
                        )
                    }
                }

                // Notification settings toggles
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Notify on timelock expiry
                    NotificationToggleItem(
                        title = "Notify them when timelock expires",
                        description = "Send an email to the Beneficiary or the Trustee once the timelock has expired.",
                        checked = notifyOnTimelockExpiry,
                        onCheckedChange = onNotifyOnTimelockExpiryChange
                    )

                    // Notify on wallet changes
                    NotificationToggleItem(
                        title = "Notify them when wallet changes",
                        description = "Send an email to the Beneficiary or the Trustee whenever the wallet configuration changes (including today).",
                        checked = notifyOnWalletChanges,
                        onCheckedChange = onNotifyOnWalletChangesChange
                    )

                    // Include wallet configuration
                    NotificationToggleItem(
                        title = "Include wallet configuration",
                        description = "Attach the wallet configuration (BSMS file) in the email notifications to the Beneficiary or the Trustee. The wallet configuration is needed to claim without the Nunchuk service.",
                        checked = includeWalletConfiguration,
                        onCheckedChange = onIncludeWalletConfigurationChange
                    )

                    // Email me wallet config
                    NotificationToggleItem(
                        title = "Email me a copy of the wallet config when wallet changes",
                        description = "Automatically get a backup of the wallet config in the owner's email inbox.",
                        checked = emailMeWalletConfig,
                        onCheckedChange = onEmailMeWalletConfigChange
                    )
                }

                Spacer(modifier = Modifier.weight(1.0f))
            }
        }
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = NunchukTheme.typography.body
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = NunchukTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        NcSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@PreviewLightDark
@Composable
private fun InheritanceNotificationSettingsScreenPreview() {
    InheritanceNotificationSettingsScreenContent(
        emails = listOf(
            "jayce@nunchuk.io"
        )
    )
}
