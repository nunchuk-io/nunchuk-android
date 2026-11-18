package com.nunchuk.android.settings.walletsecurity.biometric

import android.app.Dialog
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.auth.R
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.biometric.BiometricPromptManager
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.settings.displaysettings.AppearanceScreen
import com.nunchuk.android.settings.displaysettings.DisplaySettingsContent
import com.nunchuk.android.settings.R as SettingsR
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BiometricActivity : BaseComposeActivity() {

    private val biometricPromptManager by lazy { BiometricPromptManager(this) }
    private val viewModel: BiometricViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, backCallback)

        flowObserver(viewModel.event) {
            when (it) {
                is BiometricEvent.SignOut -> {
                    navigator.openSignInScreen(this)
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // UNDISPATCHED: be subscribed before showBiometricPrompt() sends anything.
                launch(start = CoroutineStart.UNDISPATCHED) { collectPromptResults() }
                // Prompt only once results are being collected: the manager reports "no biometric
                // available" synchronously on an unbuffered channel, so prompting from onCreate
                // dropped that result and left a blank screen. Running on every start also asks
                // again after the app comes back from background (the system cancels the prompt).
                if (retryDialog?.isShowing != true) {
                    biometricPromptManager.showBiometricPrompt()
                }
            }
        }
    }

    private suspend fun collectPromptResults() {
        biometricPromptManager.promptResults.collect {
            when (it) {
                is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                    finish()
                }
                // This activity has no content and back is disabled, so every result that
                // does not unlock must offer a way out, or the user is left on a blank screen.
                is BiometricPromptManager.BiometricResult.AuthenticationError -> when {
                    // App went to background: the prompt is shown again on the next start.
                    it.isCancelledBySystem -> Unit
                    // On Cancel the system's error text is just the button label, so use our own copy.
                    it.isCancelledByUser -> showRetryOrSignInDialog(
                        getString(SettingsR.string.nc_biometric_unlock_cancelled)
                    )
                    else -> showRetryOrSignInDialog(it.error)
                }
                is BiometricPromptManager.BiometricResult.FeatureUnavailable,
                is BiometricPromptManager.BiometricResult.HardwareUnavailable -> {
                    showRetryOrSignInDialog(getString(SettingsR.string.nc_biometric_unlock_unavailable))
                }
                is BiometricPromptManager.BiometricResult.AuthenticationNotSet -> {
                    showRetryOrSignInDialog(getString(R.string.nc_biometric_is_not_enable_this_device))
                }
                else -> {}
            }
        }
    }

    private var retryDialog: Dialog? = null

    private fun showRetryOrSignInDialog(message: String) {
        retryDialog?.dismiss()
        retryDialog = NCInfoDialog(this)
            .showDialog(
                message = message,
                btnYes = getString(R.string.nc_try_again),
                btnInfo = getString(R.string.nc_sign_in_using_password),
                onYesClick = {
                    biometricPromptManager.showBiometricPrompt()
                },
                onInfoClick = {
                    viewModel.signOut()
                }
            )
    }

    private val backCallback = object : androidx.activity.OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Disable back press
        }
    }

}