package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage

import android.nfc.tech.IsoDep
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.compose.signer.TransactionSignerView
import com.nunchuk.android.core.R
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.flow.filter
import com.nunchuk.android.main.R as MainR

@Composable
fun VerifyInheritanceMessageScreen(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState,
    message: String,
    signer: SignerModel,
    onBackPressed: () -> Unit = {},
    onContinue: () -> Unit = {},
    onSignClick: (String) -> Unit = {},
) {
    val viewModel =
        hiltViewModel<VerifyInheritanceMessageViewModel, VerifyInheritanceMessageViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(signer, message)
            }
        )
    val activity = LocalActivity.current as ComponentActivity
    val nfcViewModel = hiltViewModel<NfcViewModel>(viewModelStoreOwner = activity)
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is VerifyInheritanceMessageEvent.ShowError -> {
                    snackState.showNunchukSnackbar(
                        message = event.message,
                        type = NcToastType.ERROR
                    )
                }
                VerifyInheritanceMessageEvent.SignSuccess -> {

                }
                VerifyInheritanceMessageEvent.NoSignatureDetected -> {

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

    VerifyInheritanceMessageContent(
        modifier = modifier,
        snackState = snackState,
        message = message,
        signer = signer,
        uiState = uiState,
        onBackPressed = onBackPressed,
        onContinue = onContinue,
        onSignClick = { messageToSign ->
            if (signer.type == SignerType.NFC) {
                (activity as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_NFC_HEALTH_CHECK)
            } else {
                onSignClick(messageToSign)
            }
        },
    )
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
        when(loadingType) {
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
                    onSignClick(message)
                }
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
