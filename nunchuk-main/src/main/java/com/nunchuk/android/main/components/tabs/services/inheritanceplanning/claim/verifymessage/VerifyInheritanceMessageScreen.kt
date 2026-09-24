package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.compose.signer.TransactionSignerView
import com.nunchuk.android.core.R
import com.nunchuk.android.core.bitbox.BitBoxSignMessageSheet
import com.nunchuk.android.core.ledger.LedgerSignMessageSheet
import com.nunchuk.android.core.data.model.membership.SigningChallengeMessage
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.formattedName
import com.nunchuk.android.core.util.isBitBox
import com.nunchuk.android.core.util.isJadeAirgap
import com.nunchuk.android.core.util.isLedger
import com.nunchuk.android.core.util.isTrezor
import com.nunchuk.android.core.util.openTrezorSuiteLink
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimData
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceEvent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimUiState
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.model.SignFlowType
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCInputDialog
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import com.nunchuk.android.main.R as MainR
import com.nunchuk.android.transaction.R as TransactionR
import com.nunchuk.android.core.util.isPassportAirgap
import com.nunchuk.android.core.util.isKruxAirgap
import com.nunchuk.android.core.util.signsByExportImport
import com.nunchuk.android.core.util.isColdCard

@Composable
fun VerifyInheritanceMessageScreen(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState,
    sharedUiState: ClaimUiState,
    claimData: ClaimData,
    navigator: NunchukNavigator,
    onBackPressed: () -> Unit = {},
    addMoreSigner: () -> Unit = {},
    onSuccess: (InheritanceAdditional) -> Unit = {},
    onSigned: (String) -> Unit = {},
    onNavigateToExportComplete: () -> Unit = {},
    onEventHandled: () -> Unit = {},
) {
    val localSigner = claimData.signers.last()
    val path =
        claimData.keyOrigins.find { it.xfp == localSigner.fingerPrint }?.derivationPath.orEmpty()
    val signer =
        if (localSigner.isMasterSigner) localSigner.copy(derivationPath = path) else localSigner
    val challenge = claimData.challenge
    val signingChallengeMessage = SigningChallengeMessage(
        id = challenge?.id,
        message = challenge?.message
    )

    val viewModel =
        hiltViewModel<VerifyInheritanceMessageViewModel, VerifyInheritanceMessageViewModel.Factory> { factory ->
            factory.create(signer, signingChallengeMessage)
        }
    val importOrExportTransactionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val signature = result.data?.getStringExtra(GlobalResultKey.SIGNATURE)
            signature?.let {
                viewModel.importSignature(it)
            }
        }
    }
    val importFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importSignatureFromFile(it)
        }
    }
    val activity = LocalActivity.current as ComponentActivity
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val data = uiState.messageFile.orEmpty()
            if (data.isNotEmpty()) {
                viewModel.saveLocalFile(data)
            }
        }
    }
    val nfcViewModel = hiltViewModel<NfcViewModel>(viewModelStoreOwner = activity)
    val coroutineScope = rememberCoroutineScope()
    var showSigningOptionsSheet by remember { mutableStateOf(false) }
    var showLedgerSheet by remember { mutableStateOf(false) }

    // Everything the Export / Import options sheet can do for this signer. Saving, sharing and
    // picking a file are the same for every device that signs from a memory card; QR and NFC are
    // where they differ, and Ledger/BitBox/Trezor never come here at all.
    val fileSigningCallbacks = ColdCardSigningCallbacks(
        onImportViaFile = { importFileLauncher.launch("*/*") },
        onSaveFile = {
            coroutineScope.launch {
                val data = viewModel.generateMessageFileIfNeeded()
                if (data.isNotEmpty()) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        if (ContextCompat.checkSelfPermission(
                                activity,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            viewModel.saveLocalFile(data)
                        }
                    } else {
                        viewModel.saveLocalFile(data)
                    }
                }
            }
        },
        onShareFile = {
            coroutineScope.launch {
                val data = viewModel.generateMessageFileIfNeeded()
                if (data.isNotEmpty()) {
                    viewModel.exportTransactionToFile(data)
                }
            }
        },
    )
    // One plain-text QR out, the signature QR back — Jade and Krux. [deviceTag] only names the
    // device on the export screen.
    val airgapQrSigningCallbacks = { deviceTag: SignerTag ->
        ColdCardSigningCallbacks(
            onExportViaQr = {
                coroutineScope.launch {
                    val request = viewModel.airgapSignMessageRequest()
                    if (request.isNotEmpty()) {
                        navigator.openExportTransactionScreen(
                            launcher = importOrExportTransactionLauncher,
                            activityContext = activity,
                            txToSign = request,
                            signFlowType = SignFlowType.ClaimAirgapMessage,
                            deviceName = deviceTag.formattedName,
                        )
                    }
                }
            },
            onImportViaQr = {
                navigator.openImportTransactionScreen(
                    launcher = importOrExportTransactionLauncher,
                    activityContext = activity,
                    signFlowType = SignFlowType.ClaimAirgapMessage
                )
            },
        )
    }
    val signingCallbacks = when {
        signer.isJadeAirgap -> airgapQrSigningCallbacks(SignerTag.JADE)
        signer.isPassportAirgap -> fileSigningCallbacks
        signer.isKruxAirgap -> airgapQrSigningCallbacks(SignerTag.KRUX).let { qr ->
            fileSigningCallbacks.copy(onExportViaQr = qr.onExportViaQr, onImportViaQr = qr.onImportViaQr)
        }
        // Coldcard: BBQR and NFC on top of the file.
        else -> fileSigningCallbacks.copy(
            onExportViaQr = {
                coroutineScope.launch {
                    val data = viewModel.generateMessageFileIfNeeded()
                    if (data.isNotEmpty()) {
                        navigator.openExportTransactionScreen(
                            launcher = importOrExportTransactionLauncher,
                            activityContext = activity,
                            txToSign = data,
                            signFlowType = SignFlowType.ClaimDummy,
                        )
                    }
                }
            },
            onExportViaNfc = {
                coroutineScope.launch {
                    val data = viewModel.generateMessageFileIfNeeded()
                    if (data.isNotEmpty()) {
                        (activity as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_GENERATE_HEAL_CHECK_MSG)
                    }
                }
            },
            onImportViaQr = {
                navigator.openImportTransactionScreen(
                    launcher = importOrExportTransactionLauncher,
                    activityContext = activity,
                    signFlowType = SignFlowType.ClaimDummy
                )
            },
            onImportViaNfc = {
                (activity as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE)
            },
        )
    }

    LaunchedEffect(sharedUiState.event) {
        val event = sharedUiState.event
        when (event) {
            is ClaimInheritanceEvent.ImportSignature -> {
                signingCallbacks.onImport(event.via)
                onEventHandled()
            }

            else -> {}
        }
    }

    LaunchedEffect(uiState.signedMessage) {
        val signedMessage = uiState.signedMessage
        if (signedMessage != null) {
            onSigned(signedMessage.signature)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is VerifyInheritanceMessageEvent.ShowError -> {
                    snackState.showNunchukSnackbar(
                        message = event.message,
                        type = NcToastType.ERROR
                    )
                }

                VerifyInheritanceMessageEvent.NoSignatureDetected -> {

                }

                is VerifyInheritanceMessageEvent.GetInheritanceClaimStateSuccess -> {
                    onSuccess(event.inheritanceAdditional)
                }

                is VerifyInheritanceMessageEvent.NfcError -> if (!nfcViewModel.handleNfcError(event.e)) {
                    snackState.showNunchukSnackbar(
                        message = event.e.message.orUnknownError(),
                        type = NcToastType.ERROR
                    )
                }

                is VerifyInheritanceMessageEvent.ExportToFileSuccess -> {
                    val controller = IntentSharingController.from(activity)
                    controller.shareFile(event.filePath)
                    onNavigateToExportComplete()
                }

                is VerifyInheritanceMessageEvent.SaveLocalFile -> {
                    if (event.isSuccess) {
                        snackState.showNunchukSnackbar(
                            message = activity.getString(R.string.nc_save_file_success),
                            type = NcToastType.SUCCESS
                        )
                        onNavigateToExportComplete()
                    } else {
                        snackState.showNunchukSnackbar(
                            message = activity.getString(R.string.nc_save_file_failed),
                            type = NcToastType.ERROR
                        )
                    }
                }

                VerifyInheritanceMessageEvent.ExportTransactionToColdcardSuccess -> {
                    snackState.showNunchukSnackbar(
                        message = activity.getString(R.string.nc_transaction_exported),
                        type = NcToastType.SUCCESS
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        nfcViewModel.nfcScanInfo
            .filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_HEALTH_CHECK }
            .collect { nfcScanInfo ->
                val isoDep = IsoDep.get(nfcScanInfo.tag) ?: return@collect
                viewModel.signMessageByTapSigner(isoDep, nfcViewModel.inputCvc.orEmpty())
                nfcViewModel.clearScanInfo()
            }
    }

    LaunchedEffect(Unit) {
        nfcViewModel.nfcScanInfo
            .filter { it.requestCode == BaseNfcActivity.REQUEST_GENERATE_HEAL_CHECK_MSG }
            .collect { scanInfo ->
                val ndef = Ndef.get(scanInfo.tag) ?: return@collect
                viewModel.handleExportTransactionToMk4(ndef)
                nfcViewModel.clearScanInfo()
            }
    }

    LaunchedEffect(Unit) {
        nfcViewModel.nfcScanInfo
            .filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE }
            .collect { scanInfo ->
                viewModel.importSignatureFromNfc(scanInfo.records)
                nfcViewModel.clearScanInfo()
            }
    }

    VerifyInheritanceMessageContent(
        modifier = modifier,
        snackState = snackState,
        message = signingChallengeMessage.message.orEmpty(),
        signer = signer,
        uiState = uiState,
        onBackPressed = onBackPressed,
        onContinue = {
            if (claimData.requiredKeyCount > claimData.signers.size) {
                addMoreSigner()
            } else {
                viewModel.getInheritanceClaimState(
                    magic = claimData.magic,
                    signers = claimData.signers,
                    signatures = claimData.signatures
                )
            }
        },
        onSignClick = { messageToSign ->
            when {
                signer.type == SignerType.NFC -> {
                    (activity as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_NFC_HEALTH_CHECK)
                }
                signer.type == SignerType.SOFTWARE -> {
                    if (viewModel.needPassphrase()) {
                        NCInputDialog(activity).showDialog(
                            title = activity.getString(TransactionR.string.nc_transaction_enter_passphrase),
                            onConfirmed = { passphrase ->
                                viewModel.handlePassphrase(passphrase)
                            }
                        )
                    } else {
                        viewModel.signMessageBySoftware()
                    }
                }
                // Coldcard, Jade, Passport, Krux: export the request, import the signature. Which
                // routes the sheet offers and what they do follow the device (signingCallbacks).
                signer.signsByExportImport -> showSigningOptionsSheet = true
                // Ledger and BitBox sign in-app over BLE/USB; Trezor through Trezor Suite. All
                // three hand back a bare signature, like a Coldcard's QR does.
                signer.isLedger -> showLedgerSheet = true
                signer.isBitBox -> viewModel.requestSignMessageByBitBox()
                signer.isTrezor -> viewModel.requestSignMessageByTrezor()
                else -> Unit
            }
        },
    )

    ColdCardSigningBottomSheets(
        isMessage = true,
        isQrOnly = signer.isJadeAirgap,
        isFileOnly = signer.isPassportAirgap,
        supportsNfc = signer.isColdCard,
        showColdCardOptions = showSigningOptionsSheet,
        onDismissColdCardOptions = { showSigningOptionsSheet = false },
        callbacks = signingCallbacks,
    )

    if (showLedgerSheet) {
        LedgerSignMessageSheet(
            masterFingerprint = signer.fingerPrint,
            derivationPath = signer.derivationPath,
            message = signingChallengeMessage.message.orEmpty(),
            onDismiss = { showLedgerSheet = false },
            onSignature = viewModel::importSignature,
        )
    }

    uiState.bitBoxSignMessagePath?.let { path ->
        BitBoxSignMessageSheet(
            masterFingerprint = signer.fingerPrint,
            derivationPath = path,
            message = signingChallengeMessage.message.orEmpty(),
            onDismiss = viewModel::dismissBitBoxSheet,
            onSignature = viewModel::importSignature,
        )
    }

    uiState.trezorSuiteDeeplink?.let { deeplink ->
        NcConfirmationDialog(
            title = stringResource(id = R.string.nc_confirmation),
            message = stringResource(id = TransactionR.string.nc_open_trezor_suite_continue_signing_message),
            positiveButtonText = stringResource(id = TransactionR.string.nc_open_trezor_suite),
            negativeButtonText = stringResource(id = R.string.nc_cancel),
            isPositiveButtonWrapContent = true,
            onPositiveClick = {
                activity.openTrezorSuiteLink(deeplink)
                viewModel.dismissTrezorSuiteDialog()
            },
            onDismiss = viewModel::dismissTrezorSuiteDialog,
        )
    }
}

@Composable
fun VerifyInheritanceMessageContent(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState,
    message: String,
    signer: SignerModel,
    uiState: VerifyInheritanceMessageUiState,
    onBackPressed: () -> Unit = {},
    onContinue: () -> Unit = {},
    onSignClick: (String) -> Unit = {},
) {
    val isMessageSigned = uiState.signedMessage != null

    val loadingType = uiState.loadingType
    if (loadingType != null) {
        when (loadingType) {
            LoadingType.Normal -> NcLoadingDialog()
            LoadingType.Nfc -> NcLoadingDialog(
                title = stringResource(id = R.string.nc_please_wait),
                customMessage = stringResource(id = R.string.nc_keep_holding_nfc)
            )

            LoadingType.ColdCard -> NcLoadingDialog(
                title = stringResource(id = R.string.nc_data_transfer_in_progress),
                customMessage = stringResource(id = R.string.nc_keep_hold_coldcard_until_finish)
            )
        }
    }

    NcScaffold(
        modifier = modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcImageAppBar(
                backgroundRes = MainR.drawable.bg_claim_inheritance_sign_message,
                onClosedClicked = onBackPressed,
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isMessageSigned,
                    onClick = onContinue
                ) {
                    Text(text = stringResource(R.string.nc_text_continue))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(MainR.string.nc_verify_inheritance_key),
                style = NunchukTheme.typography.heading,
                modifier = Modifier.padding(top = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(MainR.string.nc_sign_message),
                style = NunchukTheme.typography.title,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = colorResource(id = R.color.nc_grey_light),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 40.dp)
            ) {
                Text(
                    text = message,
                    style = NunchukTheme.typography.body
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TransactionSignerView(
                modifier = Modifier.fillMaxWidth(),
                signer = signer,
                showValueKey = false,
                isSigned = isMessageSigned,
                canSign = true,
                onSignClick = { onSignClick(message) }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun VerifyInheritanceMessageContentPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        VerifyInheritanceMessageContent(
            snackState = remember { SnackbarHostState() },
            message = "I want to claim an inheritance. Dec 05, 2025.",
            signer = signer,
            uiState = VerifyInheritanceMessageUiState(),
        )
    }
}
