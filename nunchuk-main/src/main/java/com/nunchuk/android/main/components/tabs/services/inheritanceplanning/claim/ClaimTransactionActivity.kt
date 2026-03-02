package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimTransactionViewModel.LoadingType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage.ColdCardSigningBottomSheets
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage.ColdCardSigningCallbacks
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.ClaimTransactionArgs
import com.nunchuk.android.share.model.SignFlowType
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.transaction.components.details.TransactionDetailView
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCInputDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import com.nunchuk.android.core.R as CoreR

@AndroidEntryPoint
class ClaimTransactionActivity : BaseNfcActivity<ViewBinding>() {

    override fun initializeBinding(): ViewBinding = ViewBinding {
        val args = ClaimTransactionArgs.deserializeFrom(intent)

        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ClaimTransactionScreen(
                    args = args,
                    activity = this@ClaimTransactionActivity,
                    navigator = this@ClaimTransactionActivity.navigator
                )
            }
        }
    }.also {
        enableEdgeToEdge()
    }
}

@Composable
private fun ClaimTransactionScreen(
    args: ClaimTransactionArgs,
    activity: ClaimTransactionActivity,
    navigator: NunchukNavigator
) {
    val viewModel =
        hiltViewModel<ClaimTransactionViewModel, ClaimTransactionViewModel.Factory> { factory ->
            factory.create(args)
        }

    val nfcViewModel = hiltViewModel<NfcViewModel>(viewModelStoreOwner = activity)

    val context = LocalContext.current

    val state by viewModel.state.collectAsStateWithLifecycle()
    val miniscriptState by viewModel.miniscriptState.collectAsStateWithLifecycle()
    val needPassphrase by viewModel.needPassphrase.collectAsStateWithLifecycle()
    val loadingType by viewModel.loadingType.collectAsStateWithLifecycle()
    val claimError by viewModel.claimError.collectAsStateWithLifecycle()
    var showColdCardOptionsSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(claimError) {
        claimError?.let { error ->
            snackbarHostState.showNunchukSnackbar(
                message = error,
                type = NcToastType.ERROR
            )
        }
    }
    val coroutineScope = rememberCoroutineScope()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val psbt = state.transaction.psbt
            if (psbt.isNotEmpty()) {
                viewModel.saveLocalFile(psbt)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ClaimTransactionEvent.SaveLocalFile -> {
                    if (event.isSuccess) {
                        snackbarHostState.showNunchukSnackbar(
                            message = context.getString(R.string.nc_save_file_success),
                            type = NcToastType.SUCCESS
                        )
                    } else {
                        snackbarHostState.showNunchukSnackbar(
                            message = context.getString(R.string.nc_save_file_failed),
                            type = NcToastType.ERROR
                        )
                    }
                }

                is ClaimTransactionEvent.ExportToFileSuccess -> {
                    IntentSharingController.from(activity).shareFile(event.filePath)
                }

                is ClaimTransactionEvent.ShowError -> {
                    snackbarHostState.showNunchukSnackbar(
                        message = event.message,
                        type = NcToastType.ERROR
                    )
                }

                ClaimTransactionEvent.ExportTransactionToMk4Success -> {
                    snackbarHostState.showNunchukSnackbar(
                        message = context.getString(R.string.nc_transaction_exported),
                        type = NcToastType.SUCCESS
                    )
                }

                ClaimTransactionEvent.ImportTransactionFromMk4Success -> {
                    snackbarHostState.showNunchukSnackbar(
                        message = context.getString(com.nunchuk.android.transaction.R.string.nc_signed_transaction),
                        type = NcToastType.SUCCESS
                    )
                }
            }
        }
    }

    val importOrExportTransactionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val transaction =
                result.data?.parcelable<Transaction>(GlobalResultKey.TRANSACTION_EXTRA)
            transaction?.let { viewModel.updateTransaction(it) }
        }
    }

    // Handle NFC scanning for tap signer
    LaunchedEffect(Unit) {
        nfcViewModel.nfcScanInfo
            .filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION }
            .collect { scanInfo ->
                val isoDep = IsoDep.get(scanInfo.tag) ?: return@collect
                viewModel.signTapSignerPsbt(
                    isoDep = isoDep,
                    cvc = nfcViewModel.inputCvc.orEmpty()
                )
                nfcViewModel.clearScanInfo()
            }
    }

    // Handle NFC export PSBT to ColdCard Mk4
    LaunchedEffect(Unit) {
        nfcViewModel.nfcScanInfo
            .filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_EXPORT_TRANSACTION }
            .collect { scanInfo ->
                val ndef = Ndef.get(scanInfo.tag) ?: return@collect
                viewModel.handleExportTransactionToMk4(ndef)
                nfcViewModel.clearScanInfo()
            }
    }

    // Handle NFC import PSBT from ColdCard Mk4
    LaunchedEffect(Unit) {
        nfcViewModel.nfcScanInfo
            .filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE }
            .collect { scanInfo ->
                viewModel.handleImportTransactionFromMk4(scanInfo.records)
                nfcViewModel.clearScanInfo()
            }
    }

    // Handle passphrase dialog
    LaunchedEffect(needPassphrase) {
        needPassphrase?.let { id ->
            NCInputDialog(context as Activity).showDialog(
                title = context.getString(com.nunchuk.android.transaction.R.string.nc_transaction_enter_passphrase),
                onConfirmed = { passphrase ->
                    viewModel.handlePassphrase(passphrase)
                }
            )
        }
    }
    NunchukTheme {
        val type = loadingType
        if (type != null) {
            when (type) {
                LoadingType.Normal -> NcLoadingDialog()
                LoadingType.Nfc -> NcLoadingDialog(
                    title = context.getString(R.string.nc_please_wait),
                    customMessage = context.getString(R.string.nc_keep_holding_nfc)
                )

                LoadingType.ColdCard -> NcLoadingDialog(
                    title = context.getString(CoreR.string.nc_data_transfer_in_progress),
                    customMessage = context.getString(CoreR.string.nc_keep_hold_coldcard_until_finish)
                )
            }
        }
        TransactionDetailView(
            isHideChangeIndex = true, // claim off chain transaction is hide change index
            isDummyTx = false,
            walletId = "",
            txId = args.transaction.txId,
            state = state,
            miniscriptUiState = miniscriptState,
            snackbarHostState = snackbarHostState,
            isShowRetryButton = claimError != null,
            onRetryClick = { viewModel.checkAndClaimIfAllSigned(state.transaction) },
            onShowMore = { showColdCardOptionsSheet = true },
            onSignClick = { signerModel ->
                when {
                    signerModel.type == SignerType.NFC -> {
                        (activity as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION
                        )
                    }

                    signerModel.type == SignerType.SOFTWARE -> {
                        viewModel.checkSoftwarePassphrase(signerModel)
                    }

                    signerModel.type == SignerType.COLDCARD_NFC || signerModel.tags.contains(
                        SignerTag.COLDCARD
                    ) -> {
                        showColdCardOptionsSheet = true
                    }
                }
            },
            onBroadcastClick = { /* Handle broadcast click */ },
            onViewOnBlockExplorer = { /* Handle view on block explorer */ },
            onManageCoinClick = { /* Handle manage coin click */ },
            onEditNote = { /* Handle edit note */ },
            onEditChangeCoin = { /* Handle edit change coin */ },
            onCopyText = { /* Handle copy text */ },
            onPreimageSuccess = { /* Handle preimage success */ },
            onSetPendingSignNodeId = { /* Handle set pending sign node id */ }
        )

        ColdCardSigningBottomSheets(
            showColdCardOptions = showColdCardOptionsSheet,
            onDismissColdCardOptions = { showColdCardOptionsSheet = false },
            callbacks = ColdCardSigningCallbacks(
                onExportViaQr = {
                    coroutineScope.launch {
                        val psbt = state.transaction.psbt
                        if (psbt.isNotEmpty()) {
                            navigator.openExportTransactionScreen(
                                launcher = importOrExportTransactionLauncher,
                                activityContext = activity,
                                txToSign = psbt,
                                signFlowType = SignFlowType.NormalDummy,
                                isBBQR = true
                            )
                        }
                    }
                },
                onImportViaQr = {
                    navigator.openImportTransactionScreen(
                        launcher = importOrExportTransactionLauncher,
                        activityContext = activity,
                        signFlowType = SignFlowType.NormalDummy
                    )
                },
                onExportViaNfc = {
                    coroutineScope.launch {
                        val psbt = state.transaction.psbt
                        if (psbt.isNotEmpty()) {
                            (activity as NfcActionListener).startNfcFlow(
                                BaseNfcActivity.REQUEST_MK4_EXPORT_TRANSACTION
                            )
                        }
                    }
                },
                onImportViaNfc = {
                    (activity as NfcActionListener).startNfcFlow(
                        BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE
                    )
                },
                onSaveFile = {
                    val psbt = state.transaction.psbt
                    if (psbt.isNotEmpty()) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            if (ContextCompat.checkSelfPermission(
                                    activity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {
                                viewModel.saveLocalFile(psbt)
                            }
                        } else {
                            viewModel.saveLocalFile(psbt)
                        }
                    }
                },
                onShareFile = {
                    val psbt = state.transaction.psbt
                    if (psbt.isNotEmpty()) {
                        coroutineScope.launch {
                            viewModel.exportTransactionToFile(psbt)
                        }
                    }
                }
            )
        )
    }
}
