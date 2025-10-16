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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceNotificationSettings
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
                    onContinueClick = { emailSettings, emailMeWalletConfig ->
                        openReviewPlanScreen(emailSettings, emailMeWalletConfig)
                    }
                )
            }
        }
    }

    private fun openReviewPlanScreen(
        emailSettings: List<EmailNotificationSettings>,
        emailMeWalletConfig: Boolean
    ) {
        val notificationSettings = InheritanceNotificationSettings(
            emailMeWalletConfig = emailMeWalletConfig,
            perEmailSettings = emailSettings
        )

        inheritanceViewModel.setOrUpdate(
            inheritanceViewModel.setupOrReviewParam.copy(
                notificationSettings = notificationSettings
            )
        )
        if (args.isUpdateRequest || inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
            findNavController().popBackStack(R.id.inheritanceReviewPlanFragment, false)
        } else {
            findNavController().navigate(
                InheritanceNotificationSettingsFragmentDirections.actionInheritanceNotificationSettingsFragmentToInheritanceReviewPlanFragment()
            )
        }
    }
}

@Composable
fun InheritanceNotificationSettingsScreen(
    args: InheritanceNotificationSettingsFragmentArgs,
    inheritanceViewModel: InheritancePlanningViewModel,
    onContinueClick: (List<EmailNotificationSettings>, Boolean) -> Unit,
    membershipStepManager: MembershipStepManager
) {
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    val sharedUiState by inheritanceViewModel.state.collectAsStateWithLifecycle()

    // Internal state for owner's email settings
    val initialSettings = inheritanceViewModel.setupOrReviewParam.notificationSettings
        ?: InheritanceNotificationSettings()
    val perEmailSettingsMap =
        remember(inheritanceViewModel.setupOrReviewParam.notificationSettings) {
            initialSettings.perEmailSettings.associateBy { it.email }
        }
    var emailMeWalletConfig by remember {
        mutableStateOf(initialSettings.emailMeWalletConfig)
    }

    val emails = inheritanceViewModel.setupOrReviewParam.emails
    var emailSettingsList by remember {
        mutableStateOf(
            emails.map { email ->
                EmailNotificationSettings(
                    email = email,
                    notifyOnTimelockExpiry = perEmailSettingsMap[email]?.notifyOnTimelockExpiry ?: true,
                    notifyOnWalletChanges = perEmailSettingsMap[email]?.notifyOnWalletChanges ?: true,
                    includeWalletConfiguration = perEmailSettingsMap[email]?.includeWalletConfiguration ?: true
                )
            }
        )
    }

    InheritanceNotificationSettingsScreenContent(
        userEmail = sharedUiState.userEmail,
        remainTime = remainTime,
        emailSettingsList = emailSettingsList,
        planFlow = inheritanceViewModel.setupOrReviewParam.planFlow,
        isUpdateRequest = args.isUpdateRequest,
        emailMeWalletConfig = emailMeWalletConfig,
        onEmailSettingsChange = { index, settings ->
            emailSettingsList = emailSettingsList.toMutableList().apply {
                set(index, settings)
            }
        },
        onEmailMeWalletConfigChange = { emailMeWalletConfig = it },
        onContinueClick = {
            // Pass the list of email settings and owner's email config
            onContinueClick(emailSettingsList, emailMeWalletConfig)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InheritanceNotificationSettingsScreenContent(
    userEmail: String = "",
    remainTime: Int = 0,
    emailSettingsList: List<EmailNotificationSettings> = emptyList(),
    planFlow: Int = InheritancePlanFlow.NONE,
    isUpdateRequest: Boolean = false,
    emailMeWalletConfig: Boolean = true,
    onEmailSettingsChange: (Int, EmailNotificationSettings) -> Unit = { _, _ -> },
    onEmailMeWalletConfigChange: (Boolean) -> Unit = {},
    onContinueClick: () -> Unit = {},
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

                // Your notifications section
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp),
                    text = stringResource(R.string.nc_your_notifications),
                    style = NunchukTheme.typography.body
                )

                // Owner notifications container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.backgroundMidGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    // Owner email display
                    Text(
                        text = userEmail,
                        style = NunchukTheme.typography.body,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Owner's email setting
                    NotificationToggleItem(
                        title = stringResource(R.string.nc_email_me_wallet_config_when_changes),
                        description = stringResource(R.string.nc_email_me_wallet_config_when_changes_desc),
                        checked = emailMeWalletConfig,
                        onCheckedChange = onEmailMeWalletConfigChange
                    )
                }

                // Beneficiary and Trustee notifications section
                if (emailSettingsList.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp),
                        text = stringResource(R.string.nc_beneficiary_trustee_notifications),
                        style = NunchukTheme.typography.body
                    )

                    // Display each email with its own settings
                    emailSettingsList.forEachIndexed { index, emailSettings ->
                        EmailNotificationSection(
                            emailSettings = emailSettings,
                            onSettingsChange = { newSettings ->
                                onEmailSettingsChange(index, newSettings)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1.0f))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceNotificationSettingsScreenPreview() {
    InheritanceNotificationSettingsScreenContent(
        emailSettingsList = listOf(
            EmailNotificationSettings(
                email = "alice@gmail.com",
                notifyOnTimelockExpiry = true,
                notifyOnWalletChanges = true,
                includeWalletConfiguration = true
            ),
            EmailNotificationSettings(
                email = "bob245@gmail.com",
                notifyOnTimelockExpiry = true,
                notifyOnWalletChanges = true,
                includeWalletConfiguration = true
            )
        ),
    )
}
