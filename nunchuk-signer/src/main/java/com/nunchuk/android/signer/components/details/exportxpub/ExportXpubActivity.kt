package com.nunchuk.android.signer.components.details.exportxpub

import android.content.Context
import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.core.nfc.BaseComposeNfcActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_TOPUP_XPUBS
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.HardwareTaprootSupportViewModel
import com.nunchuk.android.signer.components.details.SignerDialogs
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubAction
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubExporter
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubInput
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

/**
 * Key info → More → Export XPUB, for any key with an [XpubExporter]. A key that exports its own
 * spec opens straight on the export screen; the others first pick an account or a custom path.
 * The exporter's [XpubAction]s are the only inputs handled here. Back from the export screen
 * always returns to Key info.
 */
@AndroidEntryPoint
class ExportXpubActivity : BaseComposeNfcActivity() {
    private val viewModel: ExportXpubViewModel by viewModels()
    private val taprootSupportViewModel: HardwareTaprootSupportViewModel by viewModels()
    private val dialogs = SignerDialogs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeNfcScan()
        setContent {
            val navController = rememberNavController()
            var showNewXpubNeeded by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                viewModel.event.collect { event ->
                    when (event) {
                        is ExportXpubEvent.NeedInput -> when (event.action) {
                            XpubAction.Passphrase -> dialogs.input(
                                title = getString(R.string.nc_transaction_enter_passphrase),
                                onConfirmed = { viewModel.onInput(XpubInput.Passphrase(it)) },
                            )
                            XpubAction.NfcTap -> showNewXpubNeeded = true
                        }
                        ExportXpubEvent.OpenCustomPath -> navController.navigateToExportXpubCustomPath()
                        ExportXpubEvent.OpenExport -> navController.navigateToExportXpub()
                        ExportXpubEvent.Close -> finish()
                        is ExportXpubEvent.Error -> {
                            if (!event.isNfc || !nfcViewModel.handleNfcError(event.e)) {
                                NCToastMessage(this@ExportXpubActivity).showError(event.e.message.orUnknownError())
                            }
                            if (event.close) finish()
                        }
                    }
                }
            }

            NunchukTheme {
                dialogs.Content()
                val state by viewModel.state.collectAsStateWithLifecycle()
                // Dismissing the progress dialog cancels the export it stands for.
                if (state.loading == ExportXpubLoading.DEFAULT) {
                    NcLoadingDialog(onDismiss = viewModel::cancelExport)
                }
                // A card read keeps the app's usual NFC progress dialog. It can't be dismissed:
                // the card is being held to the phone and the read can't be interrupted anyway.
                val isReadingCard = state.loading == ExportXpubLoading.NFC
                LaunchedEffect(isReadingCard) {
                    if (isReadingCard) {
                        showLoading(cancelable = false, message = getString(com.nunchuk.android.core.R.string.nc_keep_holding_nfc))
                    } else {
                        hideLoading()
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = if (viewModel.selectsAccount) ExportXpubSelectAccountRoute else ExportXpubRoute,
                ) {
                    exportXpubSelectAccount(
                        viewModel = viewModel,
                        taprootSupportViewModel = taprootSupportViewModel,
                        onBack = ::finish,
                    )
                    exportXpubCustomPath(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                    exportXpub(
                        viewModel = viewModel,
                        onBack = ::finish,
                        onCopy = ::copyXpub,
                    )
                }

                if (showNewXpubNeeded) {
                    NcConfirmationDialog(
                        title = stringResource(R.string.nc_text_info),
                        message = stringResource(com.nunchuk.android.core.R.string.nc_new_xpub_need),
                        positiveButtonText = stringResource(com.nunchuk.android.core.R.string.nc_ok),
                        negativeButtonText = stringResource(com.nunchuk.android.core.R.string.nc_cancel),
                        onPositiveClick = {
                            showNewXpubNeeded = false
                            startNfcFlow(REQUEST_NFC_TOPUP_XPUBS)
                        },
                        onDismiss = {
                            showNewXpubNeeded = false
                            viewModel.onInputCanceled()
                        },
                    )
                }
            }
        }
    }

    private fun observeNfcScan() {
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_TOPUP_XPUBS }) {
            val isoDep = IsoDep.get(it.tag) ?: return@flowObserver
            viewModel.onInput(XpubInput.Nfc(isoDep, nfcViewModel.inputCvc.orEmpty()))
            nfcViewModel.clearScanInfo()
        }
    }

    private fun copyXpub(descriptor: String) {
        copyToClipboard(label = getString(R.string.nc_export_xpub), text = descriptor)
    }

    companion object {
        fun start(
            activityContext: Context,
            isMasterSigner: Boolean,
            masterSignerId: String,
            signerType: SignerType,
            signerTags: List<SignerTag>,
            masterFingerprint: String,
            derivationPath: String,
            descriptor: String = "",
        ) {
            activityContext.startActivity(
                Intent(activityContext, ExportXpubActivity::class.java).putExtras(
                    ExportXpubArgs(
                        isMasterSigner = isMasterSigner,
                        masterSignerId = masterSignerId,
                        signerType = signerType,
                        signerTags = signerTags,
                        masterFingerprint = masterFingerprint,
                        derivationPath = derivationPath,
                        descriptor = descriptor,
                    ).toBundle()
                )
            )
        }
    }
}
