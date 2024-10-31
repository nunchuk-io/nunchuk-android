package com.nunchuk.android.main.membership.byzantine.payment.address.whitelist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSnackBarHost
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel


@Composable
fun WhitelistAddressRoute(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    whitelistAddressViewModel: WhitelistAddressViewModel = hiltViewModel(),
    openPaymentFrequencyScreen: () -> Unit,
) {
    val snackState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val state by whitelistAddressViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(
        state.openPaymentFrequentScreenEvent,
    ) {
        state.openPaymentFrequentScreenEvent?.let {
            recurringPaymentViewModel.onAddressesChange(it)
            openPaymentFrequencyScreen()
        }
        whitelistAddressViewModel.onOpenNextScreenEventConsumed()
    }
    LaunchedEffect(
        state.invalidAddressEvent,
    ) {
        state.invalidAddressEvent?.let { address ->
            snackState.showSnackbar(
                NcSnackbarVisuals(
                    type = NcToastType.ERROR,
                    message = "${context.getString(R.string.nc_transaction_invalid_address)}: $address",
                )
            )
            whitelistAddressViewModel.onInvalidAddressEventConsumed()
        }
    }
    LaunchedEffect(state.isMyWallet) {
        if(state.isMyWallet) {
            snackState.showSnackbar(
                NcSnackbarVisuals(
                    type = NcToastType.ERROR,
                    message = "${context.getString( R.string.nc_destination_cannot_be_the_same_wallet)}",
                )
            )
            whitelistAddressViewModel.onIsMyWalletEventConsumed()
        }
    }
    LaunchedEffect(state.errorMessage) {
       state.errorMessage?.let {
            snackState.showSnackbar(
                NcSnackbarVisuals(
                    type = NcToastType.ERROR,
                    message = it,
                )
            )
            whitelistAddressViewModel.onErrorMessageEventConsumed()
        }
    }
    WhitelistAddressScreen(
        uiState = state,
        snackState = snackState,
        checkAddress = {
            whitelistAddressViewModel.checkAddressValid(it, recurringPaymentViewModel.walletId)
        },
        parseBtcUri = whitelistAddressViewModel::parseBtcUri,
        onParseAddressEventConsumed = whitelistAddressViewModel::onParseAddressEventConsumed,
    )
}

@Composable
fun WhitelistAddressScreen(
    uiState: WhitelistAddressUiState = WhitelistAddressUiState(),
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    checkAddress: (List<String>) -> Unit = {},
    parseBtcUri: (String) -> Unit = {},
    onParseAddressEventConsumed: () -> Unit = {},
) {
    var addresses by rememberSaveable {
        mutableStateOf(listOf("", ""))
    }
    var batchAddress by rememberSaveable {
        mutableStateOf("")
    }
    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(0)
    }
    var addressIndex by rememberSaveable {
        mutableIntStateOf(0)
    }
    val qrLauncher = rememberLauncherForActivityResult(contract = ScanContract()) { result ->
        result.contents?.let { content ->
            parseBtcUri(content)
        }
    }
    LaunchedEffect(uiState.parseAddressEvent) {
        uiState.parseAddressEvent?.let {
            addresses = addresses.toMutableList().apply {
                set(addressIndex, it)
            }
        }
        onParseAddressEventConsumed()
    }
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_use_whitelisted_addresses),
                    textStyle = NunchukTheme.typography.titleLarge,
                    isBack = false
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = {
                        if (selectedTabIndex == 0) {
                            checkAddress(addresses)
                        } else {
                            checkAddress(batchAddress.split("\n").map { it.trim() })
                        }
                    },
                    enabled = (addresses.isNotEmpty() && addresses.all { it.isNotEmpty() }) || batchAddress.isNotEmpty()
                ) {
                    Text(text = stringResource(R.string.nc_text_continue))
                }
            },
            snackbarHost = {
                NcSnackBarHost(snackState)
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 12.dp),
                            text = stringResource(R.string.nc_enter_addresses),
                        )
                    }
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 12.dp),
                            text = stringResource(R.string.nc_batch_import),
                        )
                    }
                }
                if (selectedTabIndex == 0) {
                    EnterAddressView(
                        addresses = addresses,
                        onAddressChange = { index, s ->
                            addresses = addresses.toMutableList().apply {
                                set(index, s)
                            }
                        },
                        onRemoveAddress = { index ->
                            addresses = addresses.toMutableList().apply {
                                removeAt(index)
                            }
                        },
                        onAddNewAddress = {
                            addresses = addresses + ""
                        },
                        openScanQrCode = {
                            addressIndex = it
                            startQRCodeScan(qrLauncher)
                        }
                    )
                } else {
                    BatchImportView(batchAddress) {
                        batchAddress = it
                    }
                }
            }
        }
    }
}

@Composable
private fun EnterAddressView(
    addresses: List<String>,
    onAddressChange: (Int, String) -> Unit = { _, _ -> },
    onRemoveAddress: (Int) -> Unit = {},
    onAddNewAddress: () -> Unit = {},
    openScanQrCode: (Int) -> Unit = {},
) {
    LazyColumn {
        itemsIndexed(addresses) { index, address ->
            EnterAddressItem(
                index = index,
                value = address,
                onValueChange = { onAddressChange(index, it) },
                onRemoveAddress = { onRemoveAddress(index) },
                openScanQrCode = openScanQrCode
            )
        }

        item("add_button") {
            NcOutlineButton(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .fillMaxWidth(),
                onClick = onAddNewAddress,
            ) {
                Image(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(24.dp),
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "Icon Add",
                )
                Text(text = stringResource(R.string.nc_add_address))
            }
        }
    }
}

@Composable
private fun BatchImportView(
    addresses: String,
    onAddressesChange: (String) -> Unit = {},
) {
    NcTextField(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxSize(),
        title = stringResource(R.string.nc_addresses),
        value = addresses,
        onValueChange = onAddressesChange,
        minLines = 7,
        maxLines = 7,
        placeholder = {
            Text(
                text = stringResource(R.string.nc_batch_import_addresses_place_holder),
                style = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.textSecondary)
            )
        }
    )
}

@Preview
@Composable
fun WhitelistAddressScreenPreview() {
    WhitelistAddressScreen()
}