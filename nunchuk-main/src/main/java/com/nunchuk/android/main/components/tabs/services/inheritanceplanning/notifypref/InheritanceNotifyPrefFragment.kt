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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcCheckBox
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.contact.components.add.EmailWithState
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.EmailValidator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceNotifyPrefFragment : MembershipFragment() {

    private val viewModel: InheritanceNotifyPrefViewModel by viewModels()
    private val args: InheritanceNotifyPrefFragmentArgs by navArgs()
    private val inheritanceViewModel: InheritancePlanningViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceNotifyPrefScreen(
                    viewModel = viewModel,
                    args = args,
                    inheritanceViewModel = inheritanceViewModel,
                    onSkipClick = {
                        openReviewPlanScreen(isDiscard = true, emptyList(), false)
                    },
                    onContinueClick = { emails, isNotify ->
                        openReviewPlanScreen(
                            isDiscard = false,
                            emails,
                            isNotify
                        )
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(inheritanceViewModel.setupOrReviewParam)
    }

    private fun openReviewPlanScreen(isDiscard: Boolean, emails: List<String>, isNotify: Boolean) {
        if (!isDiscard) {
            inheritanceViewModel.setOrUpdate(
                inheritanceViewModel.setupOrReviewParam.copy(
                    isNotify = isNotify,
                    emails = emails
                )
            )
        }
        if (args.isUpdateRequest || inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
            if (inheritanceViewModel.isMiniscriptWallet()) {
                findNavController().navigate(
                    InheritanceNotifyPrefFragmentDirections.actionInheritanceNotifyPrefFragmentToInheritanceNotificationSettingsFragment(
                        isUpdateRequest = args.isUpdateRequest
                    )
                )
            } else {
                findNavController().popBackStack()
            }
        } else if (inheritanceViewModel.isMiniscriptWallet()) {
            findNavController().navigate(
                InheritanceNotifyPrefFragmentDirections.actionInheritanceNotifyPrefFragmentToInheritanceNotificationSettingsFragment()
            )
        } else {
            findNavController().navigate(
                InheritanceNotifyPrefFragmentDirections.actionInheritanceNotifyPrefFragmentToInheritanceReviewPlanFragment()
            )
        }
    }
}

@Composable
fun InheritanceNotifyPrefScreen(
    viewModel: InheritanceNotifyPrefViewModel = viewModel(),
    args: InheritanceNotifyPrefFragmentArgs,
    inheritanceViewModel: InheritancePlanningViewModel,
    onSkipClick: () -> Unit = {},
    onContinueClick: (List<String>, Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val inheritanceState by inheritanceViewModel.state.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var isEmptyError by remember { mutableStateOf(false) }

    // Listen to ViewModel events for error handling
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is InheritanceNotifyPrefEvent.ContinueClick -> {
                    onContinueClick(event.emails, event.isNotify)
                }

                InheritanceNotifyPrefEvent.AllEmailValidEvent -> {
                    showError = false
                }

                InheritanceNotifyPrefEvent.InvalidEmailEvent -> {
                    showError = true
                    isEmptyError = false
                }

                InheritanceNotifyPrefEvent.EmptyEmailError -> {
                    showError = true
                    isEmptyError = true
                }
            }
        }
    }

    // Update error state based on emails
    LaunchedEffect(state.emails) {
        val hasError = state.emails.any { it.valid.not() || EmailValidator.valid(it.email).not() }
        showError = hasError
        isEmptyError = state.emails.isEmpty()
    }

    InheritanceNotifyPrefScreenContent(
        remainTime = remainTime,
        isMiniscriptWallet = inheritanceState.walletType == WalletType.MINISCRIPT,
        emails = state.emails,
        isNotify = state.isNotify,
        inputText = inputText,
        showError = showError,
        isEmptyError = isEmptyError,
        planFlow = inheritanceViewModel.setupOrReviewParam.planFlow,
        isUpdateRequest = args.isUpdateRequest,
        onInputTextChange = { inputText = it },
        onAddEmail = { email ->
            viewModel.handleAddEmail(email)
            inputText = ""
        },
        onRemoveEmail = viewModel::handleRemove,
        onNotifyChange = viewModel::updateIsNotify,
        onContinueClick = {
            if (inputText.isNotEmpty()) {
                viewModel.handleAddEmail(inputText)
                inputText = ""
            }
            viewModel.onContinueClicked()
        },
        onSkipClick = onSkipClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InheritanceNotifyPrefScreenContent(
    remainTime: Int = 0,
    isMiniscriptWallet: Boolean = false,
    emails: List<EmailWithState> = emptyList(),
    isNotify: Boolean = false,
    inputText: String = "",
    showError: Boolean = false,
    isEmptyError: Boolean = false,
    planFlow: Int = InheritancePlanFlow.NONE,
    isUpdateRequest: Boolean = false,
    onInputTextChange: (String) -> Unit = {},
    onAddEmail: (String) -> Unit = {},
    onRemoveEmail: (EmailWithState) -> Unit = {},
    onNotifyChange: (Boolean) -> Unit = {},
    onContinueClick: () -> Unit = {},
    onSkipClick: () -> Unit = {},
) {
    var showSkipConfirmationDialog by remember { mutableStateOf(false) }
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

                Column {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onContinueClick,
                        enabled = emails.isNotEmpty()
                    ) {
                        Text(text = continueBtnText)
                    }
                    if (isSetupFlow) {
                        NcOutlineButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            onClick = {
                                if (isMiniscriptWallet) {
                                    showSkipConfirmationDialog = true
                                } else {
                                    onSkipClick()
                                }
                            },
                        ) {
                            Text(text = stringResource(R.string.nc_dont_want_any_notifications))
                        }
                    }
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

                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_inheritance_notify_pref_desc),
                    style = NunchukTheme.typography.body
                )

                // Email input section
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_beneficiary_trustee_email_address),
                    style = NunchukTheme.typography.titleSmall
                )

                // Email container with border
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .padding(horizontal = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.strokePrimary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (emails.isEmpty()) 32.dp else 16.dp)
                ) {
                    // Email chips - vertical layout
                    if (emails.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            emails.forEach { email ->
                                EmailChip(
                                    email = email,
                                    onRemove = { onRemoveEmail(email) }
                                )
                            }
                        }
                    }

                    // Email input field - no border
                    BasicTextField(
                        value = inputText,
                        onValueChange = { newText ->
                            onInputTextChange(newText)
                            // Auto-add email on comma or space
                            if (newText.isNotEmpty() && (newText.last() == ',' || newText.last() == ' ')) {
                                onAddEmail(newText.dropLast(1))
                            }
                        },
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.textPrimary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (inputText.isNotEmpty()) {
                                    onAddEmail(inputText)
                                }
                            }
                        ),
                        textStyle = NunchukTheme.typography.body,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (emails.isNotEmpty()) 8.dp else 0.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (inputText.isEmpty() && emails.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.nc_inheritance_notify_pref_hint),
                                        style = NunchukTheme.typography.body.copy(
                                            color = MaterialTheme.colorScheme.textSecondary
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // Error message
                if (showError) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp),
                        text = if (isEmptyError) stringResource(R.string.nc_please_provide_valid_email_address) else stringResource(
                            R.string.nc_contact_valid_email_address
                        ),
                        style = NunchukTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }

                // Notify checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.nc_notify_them_today),
                        style = NunchukTheme.typography.body,
                        modifier = Modifier.weight(1f)
                    )
                    NcCheckBox(
                        modifier = Modifier.size(24.dp),
                        checked = isNotify,
                        onCheckedChange = onNotifyChange,
                    )
                }

                Spacer(modifier = Modifier.weight(1.0f))

                // Warning message
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_inheritance_notify_pref_warning))),
                    type = HighlightMessageType.WARNING
                )
            }
        }

        if (showSkipConfirmationDialog) {
            NcConfirmationDialog(
                message = stringResource(R.string.nc_inheritance_skip_notifications_confirmation),
                title = stringResource(R.string.nc_confirmation),
                positiveButtonText = stringResource(R.string.nc_text_continue),
                negativeButtonText = stringResource(R.string.nc_add_email),
                onPositiveClick = {
                    showSkipConfirmationDialog = false
                    onSkipClick()
                },
                onDismiss = {
                    showSkipConfirmationDialog = false
                }
            )
        }
    }
}

@Composable
private fun EmailChip(
    email: EmailWithState,
    onRemove: () -> Unit
) {
    val backgroundColor = if (email.valid) Color(0xFFA7F0BA) else Color(0xFFFFD7D9)

    Surface(
        modifier = Modifier
            .clickable { onRemove() },
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = if (email.valid) painterResource(R.drawable.ic_check_circle_outline_24) else painterResource(
                    R.drawable.ic_error_outline_24
                ),
                contentDescription = if (email.valid) "Valid email" else "Invalid email",
                modifier = Modifier.size(20.dp)
            )

            // Email text
            Text(
                text = email.email,
                style = NunchukTheme.typography.bodySmall,
                color = Color(0xFF031F2B),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
            )

            // Right close icon
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove email",
                tint = Color(0xFF031F2B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceNotifyPrefScreenPreview() {
    InheritanceNotifyPrefScreenContent(
        planFlow = InheritancePlanFlow.SETUP,
        isUpdateRequest = false,
        isMiniscriptWallet = true,
        emails = listOf(
            EmailWithState(email = "jayce@nunchuk.io", valid = true),
            EmailWithState(email = "invalid-email", valid = false)
        )
    )
}

@PreviewLightDark
@Composable
private fun InheritanceNotifyPrefScreenUpdatePreview() {
    InheritanceNotifyPrefScreenContent(
        planFlow = InheritancePlanFlow.NONE,
        isUpdateRequest = true,
    )
}
