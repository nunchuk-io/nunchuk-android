package com.nunchuk.android.signer.portal

import android.content.Context
import android.content.Intent
import android.nfc.tech.NfcA
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.core.R
import com.nunchuk.android.core.nfc.BaseComposeNfcActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.signer.portal.intro.portalIntro
import com.nunchuk.android.signer.portal.intro.portalIntroRoute
import com.nunchuk.android.signer.portal.passphrase.navigateToSetPassphrase
import com.nunchuk.android.signer.portal.passphrase.setPassphrase
import com.nunchuk.android.signer.portal.seed.navigateToSelectNumberWord
import com.nunchuk.android.signer.portal.seed.selectNumberWord
import com.nunchuk.android.signer.portal.setup.navigateSelectSetupSeedPhrase
import com.nunchuk.android.signer.portal.setup.selectSetupSeedPhrase
import com.nunchuk.android.signer.portal.wallet.inputName
import com.nunchuk.android.signer.portal.wallet.navigateToInputName
import com.nunchuk.android.signer.portal.wallet.navigateToSelectIndex
import com.nunchuk.android.signer.portal.wallet.navigateToSelectWalletType
import com.nunchuk.android.signer.portal.wallet.selectIndex
import com.nunchuk.android.signer.portal.wallet.selectWalletType
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NUMBER_TYPE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class PortalDeviceActivity : BaseComposeNfcActivity() {
    private val viewModel: PortalDeviceViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            NunchukTheme {
                val navigationController = rememberNavController()
                val state by viewModel.state.collectAsStateWithLifecycle()
                val snackState: SnackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(state.event) {
                    val event = state.event
                    if (event != null) {
                        when (event) {
                            is PortalDeviceEvent.AddPortal -> {
                                val status = (state.event as PortalDeviceEvent.AddPortal).status
                                if (!status.initialized) {
                                    navigationController.navigateSelectSetupSeedPhrase()
                                } else {
                                    navigationController.navigateToSelectWalletType()
                                }
                            }

                            PortalDeviceEvent.RequestScan -> startNfcFlowIfNeeded()

                            PortalDeviceEvent.StartSetupWallet -> navigationController.navigateToSelectWalletType(
                                navOptions = NavOptions.Builder().setPopUpTo(portalIntroRoute, false)
                                    .build()
                            )

                            PortalDeviceEvent.IncorrectPin -> showInputCvcDialog(getString(R.string.nc_incorrect_cvc_please_try_again))
                            PortalDeviceEvent.AskPin -> showInputCvcDialog()
                            is PortalDeviceEvent.OpenSignerInfo -> {
                                val signer = event.signer
                                navigator.openSignerInfoScreen(
                                    activityContext = this@PortalDeviceActivity,
                                    isMasterSigner = signer.hasMasterSigner,
                                    id = signer.masterFingerprint,
                                    derivationPath = signer.derivationPath,
                                    justAdded = true,
                                    name = signer.name,
                                    type = signer.type,
                                    masterFingerprint = signer.masterFingerprint
                                )
                                finish()
                            }
                        }
                        viewModel.markEventHandled()
                    }
                }

                if (state.isLoading) {
                    NcLoadingDialog()
                }

                LaunchedEffect(state.message) {
                    if (state.message.isNotEmpty()) {
                        snackState.showSnackbar(
                            NcSnackbarVisuals(
                                message = state.message,
                                type = NcToastType.ERROR
                            )
                        )
                        viewModel.markMessageHandled()
                    }
                }

                BackHandler(state.isLoading) {
                    viewModel.hideLoading()
                }

                NavHost(
                    navController = navigationController,
                    startDestination = portalIntroRoute
                ) {
                    portalIntro(
                        snackState = snackState,
                        onScanPortalClicked = {
                            viewModel.setPendingAction(AddNewPortal)
                        },
                    )
                    selectSetupSeedPhrase(
                        openSetPassphraseScreen = { mnemonic, numberOfWords ->
                            navigationController.navigateToSetPassphrase(mnemonic, numberOfWords)
                        },
                        openSelectNumberOfWords = {
                            navigationController.navigateToSelectNumberWord(it)
                        }
                    )
                    selectNumberWord(
                        openSetPassphraseScreen = { mnemonic, numberOfWords ->
                            navigationController.navigateToSetPassphrase(mnemonic, numberOfWords)
                        },
                    )
                    setPassphrase(
                        onSetupPortal = { mnemonic, numberOfWords, pin ->
                            viewModel.setPendingAction(
                                SetupPortal(
                                    mnemonic,
                                    numberOfWords,
                                    pin
                                )
                            )
                        }
                    )
                    selectWalletType(
                        onSelectWalletType = { isSingleSig ->
                            viewModel.updateMultisig(!isSingleSig)
                            navigationController.navigateToSelectIndex()
                        }
                    )
                    selectIndex(
                        snackState = snackState,
                        onSelectIndex = {
                            viewModel.updateIndex(it)
                            navigationController.navigateToInputName()
                        }
                    )
                    inputName(
                        snackState = snackState,
                        onInputName = { name ->
                            viewModel.updateName(name)
                            viewModel.setPendingAction(GetXpub)
                        }
                    )
                }
            }
        }
        observer()
    }

    private fun showInputCvcDialog(errorMessage: String? = null, descMessage: String? = null) {
        NCInputDialog(this)
            .showDialog(
                title = getString(R.string.nc_enter_pin),
                onConfirmed = { cvc ->
                    if (viewModel.isConnectedToSdk) {
                        viewModel.updatePin(cvc)
                    } else {
                        startNfcFlowIfNeeded()
                    }
                },
                isMaskedInput = true,
                errorMessage = errorMessage,
                descMessage = descMessage,
                inputType = NUMBER_TYPE
            ).show()
    }

    private fun startNfcFlowIfNeeded() {
        if (viewModel.isConnectedToSdk) return
        startNfcFlow(BaseNfcActivity.REQUEST_PORTAL)
    }

    private fun observer() {
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_PORTAL }) {
            NfcA.get(it.tag)?.let { newTag -> viewModel.newTag(newTag) }
            nfcViewModel.clearScanInfo()
        }
    }

    companion object {
        fun buildIntent(activity: Context, args: PortalDeviceArgs): Intent {
            return Intent(activity, PortalDeviceActivity::class.java).apply {
                putExtras(args.toBundle())
            }
        }
    }
}