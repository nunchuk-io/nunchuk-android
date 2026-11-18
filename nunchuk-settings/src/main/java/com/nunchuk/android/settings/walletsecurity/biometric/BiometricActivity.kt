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
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.settings.displaysettings.AppearanceScreen
import com.nunchuk.android.settings.displaysettings.DisplaySettingsContent
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
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
                biometricPromptManager.promptResults.collect {
                    when (it) {
                        is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                            finish()
                        }
                        is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                            // This activity has no content, so a cancelled/failed prompt would
                            // leave the user on a blank screen with back disabled.
                            // On Cancel the system's error text is just the button label, so use our own copy.
                            showRetryOrSignInDialog(
                                if (it.isCancelledByUser) {
                                    getString(com.nunchuk.android.settings.R.string.nc_biometric_unlock_cancelled)
                                } else {
                                    it.error
                                }
                            )
                        }
                        is BiometricPromptManager.BiometricResult.FeatureUnavailable -> {
                            showToast(getString(com.nunchuk.android.settings.R.string.nc_settings_feature_unavailable))
                        }
                        is BiometricPromptManager.BiometricResult.AuthenticationNotSet -> {
                            showRetryOrSignInDialog(getString(R.string.nc_biometric_is_not_enable_this_device))
                        }
                        else -> {}
                    }
                }
            }
        }
        biometricPromptManager.showBiometricPrompt()
    }

    private var retryDialog: Dialog? = null

    override fun onRestart() {
        super.onRestart()
        // The prompt is cancelled when the app goes to background, and that result is dropped
        // because results are only collected while STARTED. Ask again when the user comes back.
        if (retryDialog?.isShowing != true) {
            biometricPromptManager.showBiometricPrompt()
        }
    }

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