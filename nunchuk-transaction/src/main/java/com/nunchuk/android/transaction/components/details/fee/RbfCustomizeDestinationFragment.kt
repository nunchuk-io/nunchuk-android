package com.nunchuk.android.transaction.components.details.fee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import com.journeyapps.barcodescanner.ScanContract
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSnackBarHost
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.transaction.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RbfCustomizeDestinationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val viewModel: RbfCustomizeDestinationViewModel = hiltViewModel()
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                RbfCustomizeDestinationContent(
                    uiState = uiState,
                    parseBtcUri = viewModel::parseBtcUri,
                    onAddressChange = viewModel::onAddressChange,
                    onHandledMessage = viewModel::onHandledMessage,
                    onHandledCheckAddressSuccess = viewModel::onHandledCheckAddressSuccess,
                    onContinue = viewModel::checkAddressValid,
                    sendResult = { address ->
                        setFragmentResult(REQUEST_KEY, Bundle().apply {
                            putString(GlobalResultKey.ADDRESS, address)
                        })
                        findNavController().popBackStack()
                    }
                )
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "RbfCustomizeDestinationFragment"
    }
}

@Composable
private fun RbfCustomizeDestinationContent(
    uiState: RbfCustomizeDestinationUiState = RbfCustomizeDestinationUiState(),
    onAddressChange: (String) -> Unit = {},
    parseBtcUri: (String) -> Unit = {},
    onHandledMessage: () -> Unit = {},
    onHandledCheckAddressSuccess: () -> Unit = {},
    onContinue: () -> Unit = {},
    sendResult: (String) -> Unit = {},
) {
    val qrLauncher = rememberLauncherForActivityResult(contract = ScanContract()) { result ->
        result.contents?.let { content ->
            parseBtcUri(content)
        }
    }
    val snackState: SnackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        if (!uiState.errorMessage.isNullOrEmpty()) {
            snackState.showSnackbar(
                NcSnackbarVisuals(
                    message = uiState.errorMessage,
                    type = NcToastType.ERROR,
                )
            )
            onHandledMessage()
        }
    }
    LaunchedEffect(uiState.checkAddressSuccess) {
        if (uiState.checkAddressSuccess) {
            sendResult(uiState.address)
            onHandledCheckAddressSuccess()
        }
    }
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = uiState.address.isNotEmpty(),
                    onClick = onContinue,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            },
            snackbarHost = { NcSnackBarHost(snackState) },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.nc_customize_destination),
                    style = NunchukTheme.typography.heading
                )

                Text(
                    text = stringResource(R.string.nc_customize_destination_desc),
                    style = NunchukTheme.typography.body,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = stringResource(R.string.nc_new_recipient_s_address),
                    style = NunchukTheme.typography.title,
                    modifier = Modifier.padding(top = 24.dp)
                )

                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    title = "",
                    value = uiState.address,
                    onValueChange = onAddressChange,
                    minLines = 3,
                    rightContent = {
                        Image(
                            modifier = Modifier
                                .clickable { startQRCodeScan(qrLauncher) }
                                .padding(16.dp),
                            painter = painterResource(id = R.drawable.ic_qr),
                            contentDescription = "QR Code"
                        )
                    }
                )
            }
        }
    }
}


@Preview
@Composable
private fun RbfCustomizeDestinationScreenPreview() {
    RbfCustomizeDestinationContent()
}