package com.nunchuk.android.signer.ledger

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
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

/** Full-screen device picker (used by the standalone add-key / health-check flow). */
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

/**
 * Bottom-sheet device picker (used by the sign-transaction flow). Same device list as the
 * full-screen picker, but wrapping content height so the sheet only takes the space it needs.
 */
@Composable
internal fun LedgerDeviceScanSheet(
    isScanning: Boolean = false,
    devices: List<LedgerDevice> = emptyList(),
    selectedAddress: String? = null,
    statusText: String = "",
    onRescan: () -> Unit = {},
    onRefreshUsb: () -> Unit = {},
    onSelectDevice: (LedgerDevice) -> Unit = {},
    onConnect: () -> Unit = {},
    @StringRes connectButtonText: Int = R.string.nc_ledger_sign_transaction,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 12.dp),
    ) {
        LedgerDeviceScanBody(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            isScanning = isScanning,
            devices = devices,
            selectedAddress = selectedAddress,
            statusText = statusText,
            onRescan = onRescan,
            onRefreshUsb = onRefreshUsb,
            onSelectDevice = onSelectDevice,
        )
        LedgerConnectButton(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            enabled = selectedAddress != null,
            connectButtonText = connectButtonText,
            onConnect = onConnect,
        )
    }
}

@Composable
private fun LedgerConnectButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    @StringRes connectButtonText: Int,
    onConnect: () -> Unit,
) {
    NcPrimaryDarkButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onConnect,
    ) {
        Text(text = stringResource(id = connectButtonText))
    }
}

/** Shared body: heading + Bluetooth / USB device sections. Layout/scroll comes from [modifier]. */
@Composable
private fun LedgerDeviceScanBody(
    modifier: Modifier = Modifier,
    isScanning: Boolean = false,
    devices: List<LedgerDevice> = emptyList(),
    selectedAddress: String? = null,
    statusText: String = "",
    onRescan: () -> Unit = {},
    onRefreshUsb: () -> Unit = {},
    onSelectDevice: (LedgerDevice) -> Unit = {},
) {
    val bluetoothDevices = devices.filter { it.transport == LedgerTransportKind.BLE }
    val usbDevices = devices.filter { it.transport == LedgerTransportKind.USB }
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(id = R.string.nc_ledger_connect_a_device),
            style = NunchukTheme.typography.heading,
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(id = R.string.nc_ledger_connect_a_device_desc),
            style = NunchukTheme.typography.body,
        )

        // Bluetooth section header + rescan trigger.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.nc_ledger_bluetooth_devices),
                style = NunchukTheme.typography.title,
            )
            val rotation = if (isScanning) {
                val transition = rememberInfiniteTransition(label = "rescan")
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 900, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "rescan-rotation",
                ).value
            } else {
                0f
            }
            NcIcon(
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
                    .clickable(enabled = !isScanning, onClick = onRescan),
                painter = painterResource(id = R.drawable.ic_ledger_rescan),
                contentDescription = "Rescan",
            )
        }

        bluetoothDevices.forEach { device ->
            LedgerDeviceRow(
                device = device,
                selected = device.id == selectedAddress,
                onClick = { onSelectDevice(device) },
            )
        }

        // Bluetooth status / hint (e.g. "Turn on Bluetooth…", connection progress).
        if (statusText.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = statusText,
                style = NunchukTheme.typography.bodySmall
                    .copy(color = MaterialTheme.colorScheme.textSecondary),
            )
        }

        // USB section (Android supports USB-HID; list attached Ledgers).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.nc_ledger_usb_devices),
                style = NunchukTheme.typography.title,
            )
            NcIcon(
                modifier = Modifier
                    .size(24.dp)
                    .alpha(if (isScanning) 0.5f else 1f)
                    .clickable(enabled = !isScanning, onClick = onRefreshUsb),
                painter = painterResource(id = R.drawable.ic_ledger_rescan),
                contentDescription = "Refresh USB",
            )
        }
        if (usbDevices.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NcIcon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_usb),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textSecondary,
                )
                Text(
                    modifier = Modifier.padding(start = 12.dp),
                    text = stringResource(id = R.string.nc_ledger_usb_empty),
                    style = NunchukTheme.typography.body
                        .copy(color = MaterialTheme.colorScheme.textSecondary),
                )
            }
        } else {
            usbDevices.forEach { device ->
                LedgerDeviceRow(
                    device = device,
                    selected = device.id == selectedAddress,
                    onClick = { onSelectDevice(device) },
                )
            }
        }
    }
}

@Composable
private fun LedgerDeviceRow(
    device: LedgerDevice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcIcon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.ic_ledger_hardware),
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = device.name,
                style = NunchukTheme.typography.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(id = R.string.nc_ledger_device_available),
                style = NunchukTheme.typography.bodySmall
                    .copy(color = MaterialTheme.colorScheme.textSecondary),
            )
        }
        NcRadioButton(
            modifier = Modifier.size(24.dp),
            selected = selected,
            onClick = onClick,
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

@PreviewLightDark
@Composable
private fun LedgerDeviceScanSheetPreview() {
    NunchukTheme {
        LedgerDeviceScanSheet(
            devices = listOf(LedgerDevice("DE:F1:60:10:BA:AB", "Nano X E4F4", LedgerTransportKind.BLE)),
            selectedAddress = "DE:F1:60:10:BA:AB",
        )
    }
}
