package com.nunchuk.android.settings.walletsecurity.unlock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcCircleImage2
import com.nunchuk.android.compose.NcPasswordTextField
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.core.biometric.BiometricPromptManager
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class UnlockPinFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: UnlockPinFragmentArgs by navArgs()

    private val biometricPromptManager by lazy {
        BiometricPromptManager(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        val viewModel = viewModel<UnlockPinViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val biometricResult by biometricPromptManager.promptResults.collectAsStateWithLifecycle(null)

        var showBiometricNotEnrolledDialog by rememberSaveable {
            mutableStateOf(false)
        }


        if (state.isLoading) {
            NcLoadingDialog()
        }

        LaunchedEffect(state.event) {
            val event = state.event
            if (event != null) {
                when (event) {
                    UnlockPinEvent.PinMatched -> {
                        if (args.isRemovePin) {
                            showSuccess("PIN has been turned off")
                            findNavController().popBackStack()
                        } else {
                            requireActivity().finish()
                        }
                    }

                    UnlockPinEvent.GoToMain -> navigator.openMainScreen(
                        activityContext = requireActivity(),
                        isClearTask = true
                    )

                    UnlockPinEvent.GoToSignIn -> navigator.openSignInScreen(requireActivity())
                }
                viewModel.markEventHandled()
            }
        }

        LaunchedEffect(biometricResult) {
            if (biometricResult != null) {
                when (biometricResult) {
                    is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                        requireActivity().finish()
                    }

                    is BiometricPromptManager.BiometricResult.AuthenticationNotSet -> {
                        showBiometricNotEnrolledDialog = true
                    }

                    is BiometricPromptManager.BiometricResult.AuthenticationFailed -> {
                        viewModel.signOut()
                    }

                    is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                        viewModel.signOut()
                    }

                    else -> {}
                }
            }
        }

        if (state.showBiometricPrompt) {
            if (biometricPromptManager.checkDeviceHasBiometricEnrolled().not()) {
                showBiometricNotEnrolledDialog = true
            } else {
                viewModel.setShowBiometricPrompt(false)
                biometricPromptManager.showBiometricPrompt()
            }
        }

        if (showBiometricNotEnrolledDialog) {
            NcInfoDialog(
                message = stringResource(R.string.nc_biometric_is_not_enable_this_device),
                positiveButtonText = stringResource(R.string.nc_try_again),
                negativeButtonText = stringResource(R.string.nc_sign_in_using_password),
                onPositiveClick = {
                    viewModel.setShowBiometricPrompt(true)
                    showBiometricNotEnrolledDialog = false
                },
                onNegativeClick = {
                    viewModel.signOut()
                    showBiometricNotEnrolledDialog = false
                },
                onDismiss = {
                    showBiometricNotEnrolledDialog = false
                }
            )
        }

        UnlockPinContent(
            isRemovePinFlow = args.isRemovePin,
            state = state,
            onUnlock = { pin ->
                if (args.isRemovePin) {
                    viewModel.removePin(pin)
                } else {
                    viewModel.unlockPin(pin)
                }
            }
        )
    }
}

@Composable
fun UnlockPinContent(
    modifier: Modifier = Modifier,
    state: UnlockPinUiState = UnlockPinUiState(),
    isRemovePinFlow: Boolean = false,
    onUnlock: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var pin by rememberSaveable {
        mutableStateOf("")
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(1000L)
        runCatching {
            focusRequester.requestFocus()
        }
    }
    val isPinEnabled = state.walletSecuritySetting.protectWalletPin
    val desc = when {
        isRemovePinFlow -> stringResource(R.string.nc_to_continue_please_enter_your_pin)
        state.walletSecuritySetting.protectWalletPin -> stringResource(R.string.nc_enter_your_pin_to_continue_use_app)
        state.walletSecuritySetting.protectWalletPassword -> stringResource(R.string.nc_enter_your_password_to_continue_use_app)
        else -> stringResource(R.string.nc_enter_your_passphrase_to_continue_use_app)
    }
    val title = when {
        state.walletSecuritySetting.protectWalletPin -> stringResource(R.string.nc_enter_your_pin)
        state.walletSecuritySetting.protectWalletPassword -> stringResource(R.string.nc_enter_your_password)
        else -> stringResource(R.string.nc_transaction_enter_passphrase)
    }
    var btnMessage by remember { mutableStateOf("") }
    var enable by remember { mutableStateOf(true) }
    LaunchedEffect(state.attemptCount) {
        if (state.attemptCount >= 3) {
            enable = false
            repeat(30) {
                btnMessage = context.getString(R.string.nc_try_again_after_seconds, 30 - it)
                delay(1000L)
            }
            btnMessage = ""
            enable = true
        }
    }
    BackHandler(!isRemovePinFlow) {
        // disable back
    }
    NunchukTheme {
        NcScaffold(
            modifier = modifier.systemBarsPadding(),
            topBar = {
                if (isRemovePinFlow) {
                    NcTopAppBar(title = "")
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NcCircleImage2(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    resId = if (NunchukTheme.isDark) R.drawable.ic_pin_lock_dark else R.drawable.ic_pin_lock, size = 96.dp, iconSize = 60.dp
                )

                Text(
                    text = title,
                    style = NunchukTheme.typography.heading,
                )

                Text(
                    text = desc,
                    style = NunchukTheme.typography.body,
                )

                NcPasswordTextField(
                    modifier = Modifier.focusRequester(focusRequester),
                    title = "",
                    value = pin,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPinEnabled) KeyboardType.NumberPassword else KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    hasError = state.isFailed,
                ) {
                    pin = it
                }

                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enable && pin.isNotEmpty(),
                    onClick = { onUnlock(pin) }) {
                    Text(text = btnMessage.ifEmpty { stringResource(id = R.string.nc_text_continue) })
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun UnlockPinContentPreview(
    @PreviewParameter(SettingProvider::class) setting: WalletSecuritySetting,
) {
    UnlockPinContent(
        isRemovePinFlow = true,
        state = UnlockPinUiState(
            isFailed = true,
            attemptCount = 3,
            walletSecuritySetting = setting
        ),
    )
}

class SettingProvider : CollectionPreviewParameterProvider<WalletSecuritySetting>(
    listOf(
        WalletSecuritySetting.DEFAULT.copy(protectWalletPin = true),
        WalletSecuritySetting.DEFAULT.copy(protectWalletPassword = true),
        WalletSecuritySetting.DEFAULT.copy(protectWalletPassphrase = true)
    )
)