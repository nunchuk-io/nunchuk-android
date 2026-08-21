package com.nunchuk.android.signer.bitbox

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
import com.nunchuk.android.core.bitbox.BitBoxDevice
import com.nunchuk.android.core.hardware.HardwareConnectButton
import com.nunchuk.android.core.hardware.HardwareDeviceScanBody
import com.nunchuk.android.core.hardware.HardwareTransportKind
import com.nunchuk.android.signer.R
import kotlinx.serialization.Serializable

@Serializable
internal data object BitBoxDeviceScanRoute

/**
 * Design screen 04 — the device picker, sharing [HardwareDeviceScanBody] with Ledger as the
 * design note asks ("reuse LedgerDeviceScanBody and its Bluetooth/USB refresh behavior"). Only
 * the copy and the row icon differ.
 */
fun NavGraphBuilder.bitBoxDeviceScan(
    onBack: () -> Unit = {},
    isScanning: () -> Boolean = { false },
    devices: () -> List<BitBoxDevice> = { emptyList() },
    selectedAddress: () -> String? = { null },
    statusText: () -> String = { "" },
    isProcessing: () -> Boolean = { false },
    onRescan: () -> Unit = {},
    onRefreshUsb: () -> Unit = {},
    onSelectDevice: (BitBoxDevice) -> Unit = {},
    onConnect: () -> Unit = {},
    @StringRes connectButtonText: Int = com.nunchuk.android.core.R.string.nc_ledger_connect,
) {
    composable<BitBoxDeviceScanRoute> {
        BitBoxDeviceScanScreen(
            onBack = onBack,
            isScanning = isScanning(),
            devices = devices(),
            selectedAddress = selectedAddress(),
            statusText = statusText(),
            isProcessing = isProcessing(),
            onRescan = onRescan,
            onRefreshUsb = onRefreshUsb,
            onSelectDevice = onSelectDevice,
            onConnect = onConnect,
            connectButtonText = connectButtonText,
        )
    }
}

fun NavHostController.navigateToBitBoxDeviceScan() {
    navigate(BitBoxDeviceScanRoute)
}

@Composable
private fun BitBoxDeviceScanScreen(
    onBack: () -> Unit = {},
    isScanning: Boolean = false,
    devices: List<BitBoxDevice> = emptyList(),
    selectedAddress: String? = null,
    statusText: String = "",
    isProcessing: Boolean = false,
    onRescan: () -> Unit = {},
    onRefreshUsb: () -> Unit = {},
    onSelectDevice: (BitBoxDevice) -> Unit = {},
    onConnect: () -> Unit = {},
    @StringRes connectButtonText: Int = com.nunchuk.android.core.R.string.nc_ledger_connect,
) {
    Scaffold(
        topBar = { NcTopAppBar(title = "", onBackPress = onBack) },
        bottomBar = {
            HardwareConnectButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth(),
                enabled = selectedAddress != null,
                connectButtonText = connectButtonText,
                isBusy = isProcessing,
                onConnect = onConnect,
            )
        }
    ) { innerPadding ->
        HardwareDeviceScanBody(
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
            titleRes = com.nunchuk.android.core.R.string.nc_bitbox_connect_a_device,
            subtitleRes = com.nunchuk.android.core.R.string.nc_bitbox_connect_a_device_desc,
            usbEmptyRes = com.nunchuk.android.core.R.string.nc_bitbox_usb_empty,
            deviceIconRes = R.drawable.ic_bitbox_hardware,
        )
    }
}

@PreviewLightDark
@Composable
private fun BitBoxDeviceScanScreenPreview() {
    NunchukTheme {
        BitBoxDeviceScanScreen(
            devices = listOf(
                BitBoxDevice("DE:F1:60:10:BA:AB", "BitBox02 Nova", HardwareTransportKind.BLE)
            ),
            selectedAddress = "DE:F1:60:10:BA:AB",
        )
    }
}
