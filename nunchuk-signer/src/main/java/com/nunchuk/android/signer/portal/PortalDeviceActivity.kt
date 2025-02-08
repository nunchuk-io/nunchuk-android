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
import com.nunchuk.android.core.domain.data.AddNewPortal
import com.nunchuk.android.core.domain.data.ExportWallet
import com.nunchuk.android.core.domain.data.GetXpub
import com.nunchuk.android.core.domain.data.ImportWallet
import com.nunchuk.android.core.domain.data.SetupPortal
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.nfc.BaseComposeNfcActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.PortalDeviceEvent
import com.nunchuk.android.core.nfc.PortalDeviceViewModel
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.portal.intro.portalIntro
import com.nunchuk.android.signer.portal.intro.portalIntroRoute
import com.nunchuk.android.signer.portal.passphrase.navigateToSetPassphrase
import com.nunchuk.android.signer.portal.passphrase.setPassphrase
import com.nunchuk.android.signer.portal.seed.navigateToSelectNumberWord
import com.nunchuk.android.signer.portal.seed.selectNumberWord
import com.nunchuk.android.signer.portal.setup.navigateSelectSetupSeedPhrase
import com.nunchuk.android.signer.portal.setup.selectSetupSeedPhrase
import com.nunchuk.android.signer.portal.wallet.inputName
import com.nunchuk.android.signer.portal.wallet.inputWalletName
import com.nunchuk.android.signer.portal.wallet.navigateToInputName
import com.nunchuk.android.signer.portal.wallet.navigateToInputWalletName
import com.nunchuk.android.signer.portal.wallet.navigateToSelectIndex
import com.nunchuk.android.signer.portal.wallet.navigateToSelectWalletType
import com.nunchuk.android.signer.portal.wallet.selectIndex
import com.nunchuk.android.signer.portal.wallet.selectWalletType
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.TEXT_TYPE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class PortalDeviceActivity : BaseComposeNfcActivity() {
    private val viewModel: PortalDeviceViewModel by viewModels()
    private val args by lazy { PortalDeviceArgs.fromBundle(intent.extras!!) }
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
                                } else if (args.isMembershipFlow) {
                                    viewModel.setPendingAction(GetXpub)
                                    return@LaunchedEffect
                                } else if (args.groupId.isNotEmpty() || args.walletId.isNotEmpty()) {
                                    navigationController.navigateToSelectIndex()
                                } else {
                                    navigationController.navigateToSelectWalletType()
                                }
                            }

                            PortalDeviceEvent.RequestScan -> startNfcFlowIfNeeded()

                            PortalDeviceEvent.StartSetupWallet -> {
                                if (args.isMembershipFlow) {
                                    viewModel.setPendingAction(GetXpub)
                                    return@LaunchedEffect
                                } else if (args.groupId.isNotEmpty() || args.walletId.isNotEmpty()) {
                                    navigationController.navigateToSelectIndex(
                                        navOptions = NavOptions.Builder()
                                            .setPopUpTo(portalIntroRoute, false)
                                            .build()
                                    )
                                } else {
                                    navigationController.navigateToSelectWalletType(
                                        navOptions = NavOptions.Builder()
                                            .setPopUpTo(portalIntroRoute, false)
                                            .build()
                                    )
                                }
                            }

                            PortalDeviceEvent.IncorrectPin -> showInputCvcDialog(getString(R.string.nc_incorrect_cvc_please_try_again))
                            PortalDeviceEvent.AskPin -> showInputCvcDialog()
                            is PortalDeviceEvent.AddSignerSuccess -> {
                                val signer = event.signer
                                if (args.isMembershipFlow) {
                                    setResult(
                                        RESULT_OK,
                                        Intent().apply {
                                            putExtra(GlobalResultKey.EXTRA_SIGNER, signer)
                                        },
                                    )
                                } else if (args.groupId.isEmpty()) {
                                    navigator.openSignerInfoScreen(
                                        activityContext = this@PortalDeviceActivity,
                                        isMasterSigner = signer.hasMasterSigner,
                                        id = signer.masterFingerprint,
                                        masterFingerprint = signer.masterFingerprint,
                                        name = signer.name,
                                        type = signer.type,
                                        derivationPath = signer.derivationPath,
                                        justAdded = true
                                    )
                                }
                                finish()
                            }

                            is PortalDeviceEvent.ImportWalletSuccess -> navigationController.navigateToInputWalletName(
                                walletId = event.walletId
                            )

                            PortalDeviceEvent.ExportWalletSuccess -> {
                                setResult(RESULT_OK)
                                finish()
                            }

                            // handle in other screens
                            is PortalDeviceEvent.CheckFirmwareVersionSuccess,
                            is PortalDeviceEvent.SignTransactionSuccess,
                            is PortalDeviceEvent.UpdateFirmwareSuccess -> Unit

                            PortalDeviceEvent.GetXpubSuccess -> {
                                if (args.isMembershipFlow) {
                                    viewModel.createSignerForAssistedWallet()
                                } else {
                                    navigationController.navigateToInputName()
                                }
                            }
                        }
                        viewModel.markEventHandled()
                    }
                }

                if (state.isLoading) {
                    NcLoadingDialog(
                        onDismiss = {
                            viewModel.hideLoading()
                        },
                    )
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
                            when (args.type) {
                                PortalDeviceFlow.SETUP -> {
                                    viewModel.setPendingAction(AddNewPortal)
                                }

                                PortalDeviceFlow.RECOVER -> {
                                    viewModel.setPendingAction(ImportWallet)
                                }

                                PortalDeviceFlow.EXPORT -> {
                                    viewModel.setPendingAction(ExportWallet(args.walletId))
                                }

                                PortalDeviceFlow.RESCAN -> {
                                    viewModel.updateIndex(args.newIndex)
                                    viewModel.setPendingAction(GetXpub)
                                }
                            }
                        },
                        args = args
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
                        onSelectWalletType = { isSingleSig, addressType ->
                            viewModel.updateWalletConfig(!isSingleSig, addressType)
                            navigationController.navigateToSelectIndex()
                        }
                    )
                    selectIndex(
                        snackState = snackState,
                        onSelectIndex = {
                            viewModel.updateIndex(it)
                            viewModel.setPendingAction(GetXpub)
                        }
                    )
                    inputName(
                        snackState = snackState,
                        onInputName = { name ->
                            viewModel.updateName(name)
                        }
                    )
                    inputWalletName(
                        onUpdateWalletNameSuccess = { walletId, name ->
                            navigator.openWalletConfigScreen(
                                activityContext = this@PortalDeviceActivity,
                                walletId = walletId
                            )
                            NcToastManager.scheduleShowMessage(
                                getString(
                                    R.string.nc_txt_import_wallet_success,
                                    name
                                )
                            )
                            finish()
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
                title = getString(R.string.nc_enter_device_password),
                onConfirmed = { cvc ->
                    viewModel.updatePin(cvc)
                    startNfcFlowIfNeeded()
                },
                isMaskedInput = true,
                errorMessage = errorMessage,
                descMessage = descMessage,
                inputType = TEXT_TYPE
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