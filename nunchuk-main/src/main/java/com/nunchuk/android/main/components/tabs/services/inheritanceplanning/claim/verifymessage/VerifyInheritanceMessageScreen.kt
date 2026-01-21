package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.nunchuk.android.compose.NcSelectableBottomSheetWithIcon
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SelectableItem
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.compose.signer.TransactionSignerView
import com.nunchuk.android.core.R
import com.nunchuk.android.core.data.model.membership.SigningChallengeMessage
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimData
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.model.SignFlowType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCInputDialog
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import com.nunchuk.android.main.R as MainR
import com.nunchuk.android.transaction.R as TransactionR
import com.nunchuk.android.widget.R as WidgetR

@Composable
fun VerifyInheritanceMessageScreen(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState,
    claimData: ClaimData,
    navigator: NunchukNavigator,
    onBackPressed: () -> Unit = {},
    addMoreSigner: () -> Unit = {},
    onSuccess: (InheritanceAdditional) -> Unit = {},
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
    val exportTransactionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // handle import signature result if needed
    }
    val activity = LocalActivity.current as ComponentActivity
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val data = uiState.coldcardSignedData.orEmpty()
            if (data.isNotEmpty()) {
                viewModel.saveLocalFile(data)
            }
        }
    }
    val nfcViewModel = hiltViewModel<NfcViewModel>(viewModelStoreOwner = activity)
    val coroutineScope = rememberCoroutineScope()

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
                }

                is VerifyInheritanceMessageEvent.SaveLocalFile -> {
                    if (event.isSuccess) {
                        snackState.showNunchukSnackbar(
                            message = activity.getString(R.string.nc_save_file_success),
                            type = NcToastType.SUCCESS
                        )
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
            .filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_EXPORT_TRANSACTION }
            .collect { scanInfo ->
                val ndef = Ndef.get(scanInfo.tag) ?: return@collect
                viewModel.handleExportTransactionToMk4(ndef)
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
                viewModel.getInheritanceClaimState(claimData.magic)
            }
        },
        onExportViaQr = {
            coroutineScope.launch {
                val data = viewModel.generateColdCardSignedDataIfNeeded()
                if (data.isNotEmpty()) {
                    navigator.openExportTransactionScreen(
                        launcher = exportTransactionLauncher,
                        activityContext = activity,
                        txToSign = data,
                        signFlowType = SignFlowType.ClaimDummy,
                    )
                }
            }
        },
        onSaveFile = {
            coroutineScope.launch {
                val data = viewModel.generateColdCardSignedDataIfNeeded()
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
                val data = viewModel.generateColdCardSignedDataIfNeeded()
                if (data.isNotEmpty()) {
                    viewModel.exportTransactionToFile(data)
                }
            }
        },
        onExportViaNfc = {
            coroutineScope.launch {
                val data = viewModel.generateColdCardSignedDataIfNeeded()
                if (data.isNotEmpty()) {
                    (activity as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_MK4_EXPORT_TRANSACTION)
                }
            }
        },
        onSignClick = { messageToSign ->
            when (signer.type) {
                SignerType.NFC -> {
                    (activity as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_NFC_HEALTH_CHECK)
                }

                SignerType.SOFTWARE -> {
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

                else -> Unit
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyInheritanceMessageContent(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState,
    message: String,
    signer: SignerModel,
    uiState: VerifyInheritanceMessageUiState,
    onBackPressed: () -> Unit = {},
    onContinue: () -> Unit = {},
    onExportViaQr: () -> Unit = {},
    onExportViaNfc: () -> Unit = {},
    onSaveFile: () -> Unit = {},
    onShareFile: () -> Unit = {},
    onSignClick: (String) -> Unit = {},
) {
    val isMessageSigned = uiState.signedMessage != null

    var showColdCardOptionsSheet by remember { mutableStateOf(false) }
    var showExportOptionsSheet by remember { mutableStateOf(false) }
    var showSaveShareSheet by remember { mutableStateOf(false) }
    val coldCardOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exportOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val saveShareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                onSignClick = {
                    if (signer.type == SignerType.COLDCARD_NFC || signer.tags.contains(SignerTag.COLDCARD)) {
                        showColdCardOptionsSheet = true
                    } else {
                        onSignClick(message)
                    }
                }
            )
        }
    }

    if (showColdCardOptionsSheet) {
        NcSelectableBottomSheetWithIcon(
            sheetState = coldCardOptionsSheetState,
            items = listOf(
                SelectableItem(
                    resId = WidgetR.drawable.ic_export,
                    text = stringResource(R.string.nc_transaction_export_transaction)
                ),
                SelectableItem(
                    resId = WidgetR.drawable.ic_import,
                    text = stringResource(R.string.nc_import_signature)
                )
            ),
            onSelected = { index ->
                when (index) {
                    0 -> {
                        showColdCardOptionsSheet = false
                        showExportOptionsSheet = true
                    }

                    1 -> {
                        showColdCardOptionsSheet = false
                    }
                }
            },
            onDismiss = {
                showColdCardOptionsSheet = false
            }
        )
    }

    if (showExportOptionsSheet) {
        NcSelectableBottomSheetWithIcon(
            sheetState = exportOptionsSheetState,
            items = listOf(
                SelectableItem(
                    resId = WidgetR.drawable.ic_export,
                    text = stringResource(R.string.nc_export_via_file)
                ),
                SelectableItem(
                    resId = WidgetR.drawable.ic_qr,
                    text = stringResource(R.string.nc_export_via_qr)
                ),
                SelectableItem(
                    resId = WidgetR.drawable.ic_nfc,
                    text = stringResource(R.string.nc_export_via_nfc)
                )
            ),
            onSelected = { index ->
                showExportOptionsSheet = false
                when (index) {
                    0 -> {
                        showSaveShareSheet = true
                    }

                    1 -> onExportViaQr()
                    2 -> onExportViaNfc()
                }
            },
            onDismiss = {
                showExportOptionsSheet = false
            }
        )
    }

    if (showSaveShareSheet) {
        NcSelectableBottomSheetWithIcon(
            sheetState = saveShareSheetState,
            items = listOf(
                SelectableItem(
                    resId = WidgetR.drawable.ic_export,
                    text = stringResource(R.string.nc_save_file)
                ),
                SelectableItem(
                    resId = WidgetR.drawable.ic_share,
                    text = stringResource(R.string.nc_share_file)
                )
            ),
            onSelected = { index ->
                showSaveShareSheet = false
                when (index) {
                    0 -> onSaveFile()
                    1 -> onShareFile()
                }
            },
            onDismiss = {
                showSaveShareSheet = false
            }
        )
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
