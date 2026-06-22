package com.nunchuk.android.signer.trezor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.util.TrezorCallbackHolder
import com.nunchuk.android.core.util.openTrezorSuiteLink
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.ResultExistingKey
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class TrezorActivity : BaseComposeActivity() {
    private val taprootSupportViewModel: TrezorTaprootSupportViewModel by viewModels()
    private val deeplinkViewModel: TrezorDeeplinkViewModel by viewModels()

    @Inject
    lateinit var trezorCallbackHolder: TrezorCallbackHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isMembershipFlow = intent.getBooleanExtra(EXTRA_IS_MEMBERSHIP_FLOW, false)
        deeplinkViewModel.setMembershipFlow(isMembershipFlow)

        setContent {
            NunchukTheme {
                val taprootSupportState = taprootSupportViewModel.state.collectAsStateWithLifecycle().value
                val deeplinkState = deeplinkViewModel.state.collectAsStateWithLifecycle().value
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    deeplinkViewModel.event.collectLatest { event ->
                        when (event) {
                            is TrezorDeeplinkEvent.OpenDeeplink -> {
                                openTrezorSuiteLink(event.url)
                            }

                            TrezorDeeplinkEvent.NavigateToSetKeyName -> {
                                if (navController.currentDestination?.route != trezorSetKeyNameRoute) {
                                    navController.navigateToTrezorSetKeyName()
                                }
                            }

                            is TrezorDeeplinkEvent.OpenSignerInfo -> {
                                if (isMembershipFlow) {
                                    setResult(
                                        RESULT_OK,
                                        Intent().apply {
                                            putExtra(GlobalResultKey.EXTRA_SIGNER, event.signer)
                                        }
                                    )
                                } else {
                                    navigator.openSignerInfoScreen(
                                        activityContext = this@TrezorActivity,
                                        isMasterSigner = event.signer.hasMasterSigner,
                                        id = event.signer.masterFingerprint,
                                        masterFingerprint = event.signer.masterFingerprint,
                                        name = event.signer.name,
                                        type = event.signer.type,
                                        derivationPath = event.signer.derivationPath,
                                        justAdded = true
                                    )
                                }
                                finish()
                            }

                            is TrezorDeeplinkEvent.ShowError -> {
                                NCToastMessage(this@TrezorActivity).showError(event.message)
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    trezorCallbackHolder.callbackUri.collectLatest { callbackUri ->
                        if (deeplinkViewModel.handleCallbackUri(callbackUri)) {
                            trezorCallbackHolder.clear(callbackUri)
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = trezorActionIntroRoute
                ) {
                    trezorActionIntro(
                        onBack = { finish() },
                        onAddViaSuite = { navController.navigateToTrezorSuiteIntro() },
                        onAddViaUsb = {
                            if (isMembershipFlow) {
                                setResult(
                                    RESULT_OK,
                                    Intent().apply {
                                        putExtra(EXTRA_RESULT_ACTION, RESULT_ACTION_OPEN_USB_FLOW)
                                    }
                                )
                                finish()
                            }
                        },
                        isAddViaUsbEnabled = isMembershipFlow
                    )
                    trezorSuiteIntro(
                        onBack = { navController.popBackStack() },
                        onContinue = { navController.navigateToTrezorSelectWalletType() }
                    )
                    trezorSelectWalletType(
                        onBack = { navController.popBackStack() },
                        taprootSupportState = taprootSupportState,
                        onContinue = { isSingleSig, addressType, accountIndex ->
                            deeplinkViewModel.openTrezorSuiteDeeplink(
                                walletType = if (isSingleSig) WalletType.SINGLE_SIG else WalletType.MULTI_SIG,
                                addressType = addressType,
                                index = accountIndex
                            )
                        }
                    )
                    trezorSetKeyName(
                        defaultName = deeplinkState.defaultSignerName,
                        onBack = { navController.popBackStack() },
                        onContinue = { name ->
                            deeplinkViewModel.createTrezorSigner(name)
                        }
                    )
                }

                if (deeplinkState.existingKeyType != null && deeplinkState.pendingSigner != null) {
                    val fingerprint = deeplinkState.pendingSigner.masterFingerprint.uppercase(Locale.getDefault())
                    val messageRes = if (deeplinkState.existingKeyType == ResultExistingKey.Software) {
                        com.nunchuk.android.core.R.string.nc_existing_key_is_software_key_delete_key
                    } else {
                        com.nunchuk.android.core.R.string.nc_existing_key_change_key_type
                    }
                    NcConfirmationDialog(
                        title = stringResource(id = R.string.nc_info),
                        message = stringResource(id = messageRes, fingerprint),
                        positiveButtonText = stringResource(id = com.nunchuk.android.core.R.string.nc_text_yes),
                        negativeButtonText = stringResource(id = com.nunchuk.android.core.R.string.nc_text_no),
                        onPositiveClick = {
                            deeplinkViewModel.confirmExistingKeyDialog()
                        },
                        onDismiss = {
                            deeplinkViewModel.dismissExistingKeyDialog()
                        }
                    )
                }

                if (deeplinkState.isLoading) {
                    NcLoadingDialog(onDismiss = {})
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    companion object {
        const val EXTRA_IS_MEMBERSHIP_FLOW = "extra_is_membership_flow"
        const val EXTRA_RESULT_ACTION = "extra_result_action"
        const val RESULT_ACTION_OPEN_USB_FLOW = "result_action_open_usb_flow"

        fun buildIntent(
            activityContext: Context,
            isMembershipFlow: Boolean = false
        ): Intent {
            return Intent(activityContext, TrezorActivity::class.java).apply {
                putExtra(EXTRA_IS_MEMBERSHIP_FLOW, isMembershipFlow)
            }
        }
    }
}
