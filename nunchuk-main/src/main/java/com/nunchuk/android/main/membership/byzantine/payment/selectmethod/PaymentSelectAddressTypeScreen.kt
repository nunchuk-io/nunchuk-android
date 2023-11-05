package com.nunchuk.android.main.membership.byzantine.payment.selectmethod

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

@Composable
fun PaymentSelectAddressTypeRoute(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openWhiteListAddressScreen: () -> Unit,
    openScanQRCodeScreen: () -> Unit,
) {
    PaymentSelectAddressTypeScreen(
        openWhiteListAddressScreen = openWhiteListAddressScreen,
        openScanQRCodeScreen = openScanQRCodeScreen,
        openBsms = recurringPaymentViewModel::openBsms
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentSelectAddressTypeScreen(
    openWhiteListAddressScreen: () -> Unit = {},
    openScanQRCodeScreen: () -> Unit = {},
    openBsms : (Uri) -> Unit = {},
) {
    var useWallet by rememberSaveable {
        mutableStateOf<Boolean?>(null)
    }
    var showImportSheet by rememberSaveable {
        mutableStateOf(false)
    }
    val openBsmsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let { uri ->
            openBsms(uri)
        }
    }

    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_add_recurring_payments),
                textStyle = NunchukTheme.typography.titleLarge,
            )
        }, bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = {
                    if (useWallet == true) {
                        showImportSheet = true
                    } else {
                        openWhiteListAddressScreen()
                    }
                },
                enabled = useWallet != null
            ) {
                Text(text = stringResource(R.string.nc_text_continue))
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.nc_please_specify_the_destination),
                    style = NunchukTheme.typography.body,
                )

                AddressOption(
                    modifier = Modifier.padding(top = 16.dp),
                    isSelected = useWallet == true,
                    title = stringResource(R.string.nc_use_a_destination_wallet),
                    desc = stringResource(R.string.nc_use_a_destination_wallet_desc),
                    onClick = {
                        useWallet = true
                    }
                )

                AddressOption(
                    modifier = Modifier.padding(top = 16.dp),
                    isSelected = useWallet == false,
                    title = stringResource(R.string.nc_whitelist_address),
                    desc = stringResource(R.string.nc_whitelist_address_desc),
                    onClick = {
                        useWallet = false
                    }
                )
            }

            if (showImportSheet) {
                NcSelectableBottomSheet(
                    title = stringResource(R.string.nc_select_import_format),
                    options = listOf(
                        stringResource(R.string.nc_bsms),
                        stringResource(R.string.nc_coldcard),
                        stringResource(R.string.nc_text_wallet_qr_code),
                    ),
                    onSelected = { pos ->
                        when(pos) {
                            0 -> openBsmsLauncher.launch("*/*")
                            1 -> Unit
                            2 -> openScanQRCodeScreen()
                        }
                    },
                    onDismiss = {
                        showImportSheet = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddressOption(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    title: String,
    desc: String,
    onClick: () -> Unit = {},
) {
    NcRadioOption(modifier = modifier.fillMaxWidth(), isSelected = isSelected, onClick = onClick) {
        Text(text = title, style = NunchukTheme.typography.title)
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = desc,
            style = NunchukTheme.typography.body
        )
    }
}

@Preview
@Composable
fun PaymentSelectAddressTypeScreenPreview() {
    PaymentSelectAddressTypeScreen()
}