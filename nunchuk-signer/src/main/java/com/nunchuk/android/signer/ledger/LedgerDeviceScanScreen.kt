package com.nunchuk.android.signer.ledger

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.ledger.LedgerConnectButton
import com.nunchuk.android.core.ledger.LedgerDevice
import com.nunchuk.android.core.ledger.LedgerDeviceScanBody
import com.nunchuk.android.core.ledger.LedgerTransportKind
import com.nunchuk.android.signer.R
import kotlinx.serialization.Serializable

@Serializable
internal data object LedgerDeviceScanRoute

fun NavGraphBuilder.ledgerDeviceScan(
    onBack: () -> Unit = {},
    isScanning: () -> Boolean = { false },
    devices: () -> List<LedgerDevice> = { emptyList() },
    selectedAddress: () -> String? = { null },
    statusText: () -> String = { "" },
    onRescan: () -> Unit = {},
    onRefreshUsb: () -> Unit = {},
    onSelectDevice: (LedgerDevice) -> Unit = {},
    onConnect: () -> Unit = {},
    @StringRes connectButtonText: Int = R.string.nc_ledger_connect,
) {
    composable<LedgerDeviceScanRoute> {
        LedgerDeviceScanScreen(
            onBack = onBack,
            isScanning = isScanning(),
            devices = devices(),
            selectedAddress = selectedAddress(),
            statusText = statusText(),
            onRescan = onRescan,
            onRefreshUsb = onRefreshUsb,
            onSelectDevice = onSelectDevice,
            onConnect = onConnect,
            connectButtonText = connectButtonText,
        )
    }
}

fun NavHostController.navigateToLedgerDeviceScan() {
    navigate(LedgerDeviceScanRoute)
}

/**
 * Full-screen device picker (used by the standalone add-key / health-check flow). The
 * sign-transaction flow shows the same body in a bottom sheet — see
 * [com.nunchuk.android.core.ledger.LedgerSignTransactionSheet].
 */
@Composable
private fun LedgerDeviceScanScreen(
    onBack: () -> Unit = {},
    isScanning: Boolean = false,
    devices: List<LedgerDevice> = emptyList(),
    selectedAddress: String? = null,
    statusText: String = "",
    onRescan: () -> Unit = {},
    onRefreshUsb: () -> Unit = {},
    onSelectDevice: (LedgerDevice) -> Unit = {},
    onConnect: () -> Unit = {},
    @StringRes connectButtonText: Int = R.string.nc_ledger_connect,
) {
    Scaffold(
        topBar = {
            NcTopAppBar(
                title = "",
                onBackPress = onBack,
            )
        },
        bottomBar = {
            LedgerConnectButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth(),
                enabled = selectedAddress != null,
                connectButtonText = connectButtonText,
                onConnect = onConnect,
            )
        }
    ) { innerPadding ->
        LedgerDeviceScanBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            isScanning = isScanning,
            devices = devices,
            selectedAddress = selectedAddress,
            statusText = statusText,
            onRescan = onRescan,
            onRefreshUsb = onRefreshUsb,
            onSelectDevice = onSelectDevice,
        )
    }
}

@PreviewLightDark
@Composable
private fun LedgerDeviceScanScreenPreview() {
    NunchukTheme {
        LedgerDeviceScanScreen(
            devices = listOf(LedgerDevice("DE:F1:60:10:BA:AB", "My Ledger Stax", LedgerTransportKind.BLE)),
            selectedAddress = "DE:F1:60:10:BA:AB",
        )
    }
}
