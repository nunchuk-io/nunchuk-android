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

package com.nunchuk.android.settings.walletsecurity

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.biometric.BiometricPromptManager
import com.nunchuk.android.core.domain.membership.PasswordVerificationHelper
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.setting.BiometricConfig
import com.nunchuk.android.settings.R
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.NCWarningVerticalDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun NavController.navigateToWalletSecuritySetting() {
    navigate(WalletSecuritySettingRoute)
}

fun NavGraphBuilder.walletSecuritySettingScreen(
    activity: FragmentActivity,
    signInModeHolder: SignInModeHolder,
    passwordVerificationHelper: PasswordVerificationHelper,
    onBack: () -> Unit,
    onOpenPinStatus: () -> Unit,
) {
    composable<WalletSecuritySettingRoute> {
        val viewModel = hiltViewModel<WalletSecuritySettingViewModel>()
        val lifecycleOwner = LocalLifecycleOwner.current
        val coroutineScope = rememberCoroutineScope()
        val biometricPromptManager = remember(activity) {
            BiometricPromptManager(activity)
        }
        val enrollLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { }

        var state by remember {
            mutableStateOf(WalletSecuritySettingState())
        }
        var isTurningOnBiometric by rememberSaveable { mutableStateOf(true) }
        var biometricCheckedOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }

        fun showInputPinDialog() {
            NCInputDialog(activity).showDialog(
                title = activity.getString(R.string.nc_enter_your_pin),
                onCanceled = {
                    viewModel.updateProtectWalletPin(true)
                },
                onConfirmed = {
                    viewModel.checkWalletPin(it)
                },
            )
        }

        fun showPassphraseDialog() {
            NCInputDialog(activity).showDialog(
                title = activity.getString(R.string.nc_re_enter_your_passphrase),
                descMessage = activity.getString(R.string.nc_re_enter_your_passphrase_dialog_desc),
                onCanceled = {
                    viewModel.updateProtectWalletPassphrase(true)
                },
                onConfirmed = {
                    viewModel.confirmPassphrase(it)
                },
            )
        }

        DisposableEffect(viewModel, lifecycleOwner) {
            val stateObserver = Observer<WalletSecuritySettingState> {
                state = it
            }
            viewModel.state.observe(lifecycleOwner, stateObserver)
            onDispose {
                viewModel.state.removeObserver(stateObserver)
            }
        }

        DisposableEffect(viewModel, lifecycleOwner, activity) {
            val eventObserver = Observer<WalletSecuritySettingEvent> { event ->
                when (event) {
                    is WalletSecuritySettingEvent.Error -> {
                        NCToastMessage(activity).showError(message = event.message)
                    }

                    is WalletSecuritySettingEvent.Loading -> {
                        activity.showOrHideLoading(loading = event.loading)
                    }

                    WalletSecuritySettingEvent.UpdateConfigSuccess -> Unit

                    is WalletSecuritySettingEvent.CheckWalletPin -> {
                        if (event.match.not()) {
                            NCToastMessage(activity).showError(
                                message = activity.getString(R.string.nc_incorrect_current_pin),
                            )
                        } else if (event.isHideWalletDetailFlow) {
                            viewModel.updateHideWalletDetail()
                        }
                    }

                    WalletSecuritySettingEvent.None -> Unit

                    WalletSecuritySettingEvent.CheckPasswordSuccess,
                    WalletSecuritySettingEvent.CheckPassphraseSuccess -> {
                        if (viewModel.getWalletSecuritySetting().protectWalletPin && viewModel.isAppPinEnable()) {
                            showInputPinDialog()
                        } else {
                            viewModel.updateHideWalletDetail()
                        }
                    }

                    WalletSecuritySettingEvent.ShowBiometric -> {
                        biometricPromptManager.showBiometricPrompt()
                    }

                    is WalletSecuritySettingEvent.RequestFederatedTokenSuccess -> {
                        NCInputDialog(activity).showDialog(
                            title = activity.getString(R.string.nc_enter_confirmation_code),
                            descMessage = String.format(
                                activity.getString(R.string.nc_enter_confirmation_code_desc),
                                event.email,
                            ),
                            inputBoxTitle = activity.getString(R.string.nc_confirmation_code),
                            clickablePhrases = listOf(
                                "Resend code" to {
                                    viewModel.requestFederatedToken(true)
                                },
                            ),
                            confirmText = activity.getString(R.string.nc_text_continue),
                            onConfirmed = {
                                viewModel.registerBiometric(it)
                            },
                            onCanceled = {
                                biometricCheckedOverride = false
                                viewModel.updateProtectWalletBiometric(false)
                            },
                        )
                    }
                }
                viewModel.clearEvent()
            }

            viewModel.event.observe(lifecycleOwner, eventObserver)
            onDispose {
                viewModel.event.removeObserver(eventObserver)
            }
        }

        LifecycleResumeEffect(Unit) {
            viewModel.checkCustomPinConfig()
            onPauseOrDispose { }
        }

        LaunchedEffect(biometricPromptManager) {
            biometricPromptManager.promptResults.collectLatest { result ->
                when (result) {
                    is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                        if (isTurningOnBiometric) {
                            viewModel.requestFederatedToken(false)
                        } else {
                            biometricCheckedOverride = false
                            viewModel.updateProtectWalletBiometric(false)
                        }
                    }

                    is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                        if (isTurningOnBiometric) {
                            biometricCheckedOverride = false
                            viewModel.updateProtectWalletBiometric(false)
                            NCToastMessage(activity).showError(message = result.error)
                        } else {
                            biometricCheckedOverride = true
                            NCToastMessage(activity).showError(message = result.error)
                        }
                    }

                    is BiometricPromptManager.BiometricResult.AuthenticationFailed -> {
                        if (!isTurningOnBiometric) {
                            biometricCheckedOverride = true
                        }
                    }

                    else -> {
                        if (!isTurningOnBiometric) {
                            biometricCheckedOverride = true
                        }
                    }
                }
            }
        }

        val currentMode = signInModeHolder.getCurrentMode()
        val biometricConfig = viewModel.getCurrentBiometricConfig()
        val isCurrentAccount = biometricConfig?.userId == viewModel.getCurrentAccountId()
        val isDefaultBiometric = biometricConfig == BiometricConfig.DEFAULT
        val biometricCheckedByState = isCurrentAccount && biometricConfig?.enabled == true
        val biometricChecked = biometricCheckedOverride ?: biometricCheckedByState

        LaunchedEffect(biometricCheckedByState) {
            if (biometricCheckedOverride == biometricCheckedByState) {
                biometricCheckedOverride = null
            }
        }

        val biometricEnabledByConfig =
            isDefaultBiometric || (isCurrentAccount && biometricConfig?.enabled == true)
        val isBiometricOptionEnabled = biometricEnabledByConfig &&
            biometricPromptManager.checkHardwareSupport() &&
            currentMode != SignInMode.GUEST_MODE &&
            currentMode != SignInMode.PRIMARY_KEY

        val pinStatus = if (state.isAppPinEnable && state.isCustomPinEnable) {
            activity.getString(R.string.nc_on)
        } else {
            activity.getString(R.string.nc_off)
        }

        WalletSecuritySettingContentCompose(
            state = state,
            pinStatus = pinStatus,
            showPasswordOption = currentMode == SignInMode.EMAIL,
            showPassphraseOption = currentMode == SignInMode.PRIMARY_KEY,
            isPassphraseSwitchEnabled = state.isEnablePassphrase,
            biometricChecked = biometricChecked,
            isBiometricOptionEnabled = isBiometricOptionEnabled,
            onBack = onBack,
            onOpenPinStatus = onOpenPinStatus,
            onPasswordChanged = { enabled ->
                if (enabled.not()) {
                    passwordVerificationHelper.showPasswordVerificationDialog(
                        context = activity,
                        targetAction = TargetAction.PROTECT_WALLET,
                        coroutineScope = coroutineScope,
                        onSuccess = {
                            viewModel.updateProtectWalletPassword(false)
                        },
                        onError = { errorMessage ->
                            viewModel.updateProtectWalletPassword(true)
                            NCToastMessage(activity).showError(errorMessage)
                        },
                        onCancel = {
                            viewModel.updateProtectWalletPassword(true)
                        },
                    )
                } else if (viewModel.isAppPinEnable()) {
                    NCWarningDialog(activity).showDialog(
                        title = activity.getString(R.string.nc_text_confirmation),
                        message = activity.getString(R.string.nc_disable_pin_warning),
                        btnNo = activity.getString(R.string.nc_cancel),
                        btnYes = activity.getString(R.string.nc_text_continue),
                        onYesClick = {
                            coroutineScope.launch {
                                viewModel.updateProtectWalletPin(false).join()
                                viewModel.updateProtectWalletPassword(true)
                            }
                        },
                    )
                } else {
                    viewModel.updateProtectWalletPassword(true)
                }
            },
            onPassphraseChanged = { enabled ->
                if (enabled.not()) {
                    showPassphraseDialog()
                } else if (viewModel.isAppPinEnable()) {
                    NCWarningDialog(activity).showDialog(
                        title = activity.getString(R.string.nc_text_confirmation),
                        message = activity.getString(R.string.nc_disable_pin_warning),
                        btnNo = activity.getString(R.string.nc_cancel),
                        btnYes = activity.getString(R.string.nc_text_continue),
                        onYesClick = {
                            coroutineScope.launch {
                                viewModel.updateProtectWalletPin(false).join()
                                viewModel.updateProtectWalletPassphrase(true)
                            }
                        },
                    )
                } else {
                    viewModel.updateProtectWalletPassphrase(true)
                }
            },
            onBiometricChanged = { enabled ->
                biometricCheckedOverride = enabled
                if (enabled) {
                    isTurningOnBiometric = true
                    if (biometricPromptManager.checkDeviceHasBiometricEnrolled().not()) {
                        NCWarningVerticalDialog(activity).showDialog(
                            title = activity.getString(R.string.nc_fingerprint_not_set_up_yet),
                            message = activity.getString(R.string.nc_fingerprint_not_set_up_yet_desc),
                            btnYes = activity.getString(R.string.nc_try_again),
                            btnNo = activity.getString(R.string.nc_go_settings),
                            btnNeutral = activity.getString(R.string.nc_text_cancel),
                            onNoClick = {
                                biometricPromptManager.enrollBiometric(enrollLauncher)
                            },
                            onYesClick = {
                                biometricCheckedOverride = false
                            },
                            onNeutralClick = {
                                biometricCheckedOverride = false
                            },
                        )
                        return@WalletSecuritySettingContentCompose
                    }
                    NCWarningDialog(activity).showDialog(
                        title = activity.getString(R.string.nc_do_you_want_allow_use_fingerprint),
                        message = activity.getString(R.string.nc_do_you_want_allow_use_fingerprint_desc),
                        btnYes = activity.getString(R.string.nc_allow),
                        btnNo = activity.getString(R.string.nc_do_not_allow),
                        onYesClick = {
                            biometricPromptManager.showBiometricPrompt()
                        },
                        onNoClick = {
                            biometricCheckedOverride = false
                        },
                    )
                } else {
                    isTurningOnBiometric = false
                    biometricPromptManager.showBiometricPrompt()
                }
            },
        )
    }
}

@Composable
private fun WalletSecuritySettingContentCompose(
    state: WalletSecuritySettingState,
    pinStatus: String,
    showPasswordOption: Boolean,
    showPassphraseOption: Boolean,
    isPassphraseSwitchEnabled: Boolean,
    biometricChecked: Boolean,
    isBiometricOptionEnabled: Boolean,
    onBack: () -> Unit,
    onOpenPinStatus: () -> Unit,
    onPasswordChanged: (Boolean) -> Unit,
    onPassphraseChanged: (Boolean) -> Unit,
    onBiometricChanged: (Boolean) -> Unit,
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_security_settings),
                    textStyle = NunchukTheme.typography.titleLarge,
                    onBackPress = onBack,
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WalletSecuritySwitchItemCompose(
                    title = stringResource(R.string.nc_protect_wallets_with_fingerprint),
                    description = stringResource(R.string.nc_protect_wallets_with_fingerprint_desc),
                    checked = biometricChecked,
                    enabled = isBiometricOptionEnabled,
                    dimWhenDisabled = true,
                    onCheckedChange = onBiometricChanged,
                )

                WalletSecurityPinItemCompose(
                    status = pinStatus,
                    onClick = onOpenPinStatus,
                )

                if (showPasswordOption) {
                    WalletSecuritySwitchItemCompose(
                        title = stringResource(R.string.nc_protect_wallet_with_account_password),
                        description = stringResource(R.string.nc_protect_wallet_with_account_password_desc),
                        checked = state.walletSecuritySetting.protectWalletPassword,
                        enabled = true,
                        dimWhenDisabled = false,
                        onCheckedChange = onPasswordChanged,
                    )
                }

                if (showPassphraseOption) {
                    WalletSecuritySwitchItemCompose(
                        title = stringResource(R.string.nc_protect_wallet_with_account_passphrase),
                        description = stringResource(R.string.nc_protect_wallet_with_account_passphrase_desc),
                        checked = state.walletSecuritySetting.protectWalletPassphrase,
                        enabled = isPassphraseSwitchEnabled,
                        dimWhenDisabled = false,
                        onCheckedChange = onPassphraseChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletSecuritySwitchItemCompose(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    dimWhenDisabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val alpha = if (enabled || !dimWhenDisabled) 1f else 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                style = NunchukTheme.typography.body,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = description,
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary,
                ),
            )
        }
        NcSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun WalletSecurityPinItemCompose(
    status: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.nc_protect_app_with_pin),
                style = NunchukTheme.typography.body,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(R.string.nc_protect_app_with_pin_desc),
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary,
                ),
            )
        }
        Text(
            text = status,
            style = NunchukTheme.typography.body,
        )
        Spacer(modifier = Modifier.width(8.dp))
        NcIcon(
            painter = painterResource(id = R.drawable.ic_right_arrow_dark),
            contentDescription = stringResource(R.string.nc_protect_app_with_pin),
        )
    }
}
