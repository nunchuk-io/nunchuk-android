package com.nunchuk.android.core.ledger

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import com.nunchuk.android.widget.NCToastMessage

/**
 * Ledger "Sign transaction" bottom sheet, shown inline by the host screen (transaction detail).
 * Connects over BLE/USB in-app, verifies the device is [masterFingerprint], then
 * registers-if-needed + signs [txId] in [walletId] and imports the signed PSBT.
 *
 * [onSignSuccess] fires once the signed PSBT is imported so the host can refresh.
 */
@Composable
fun LedgerSignTransactionSheet(
    walletId: String,
    txId: String,
    masterFingerprint: String,
    onDismiss: () -> Unit,
    onSignSuccess: () -> Unit,
) {
    val action = remember(walletId, txId, masterFingerprint) {
        LedgerSheetAction.SignTransaction(
            walletId = walletId,
            txId = txId,
            masterFingerprint = masterFingerprint,
        )
    }
    LedgerSheet(
        action = action,
        onDismiss = onDismiss,
        onEvent = { event ->
            if (event is LedgerSheetEvent.SignTransactionSuccess) {
                onSignSuccess()
                onDismiss()
            }
        },
    )
}

/**
 * Ledger "Sign transaction" bottom sheet for a dummy transaction, shown inline by the host
 * screen (dummy transaction details). Same device conversation as [LedgerSignTransactionSheet]
 * — verify the device is [masterFingerprint], register the wallet if needed, sign [psbt] — but
 * the signed PSBT is handed to [onSignSuccess] instead of being imported into [walletId], so the
 * host can extract the dummy transaction signature from it.
 */
@Composable
fun LedgerSignPsbtSheet(
    walletId: String,
    psbt: String,
    masterFingerprint: String,
    onDismiss: () -> Unit,
    onSignSuccess: (signedPsbt: String) -> Unit,
) {
    val action = remember(walletId, psbt, masterFingerprint) {
        LedgerSheetAction.SignPsbt(
            walletId = walletId,
            psbt = psbt,
            masterFingerprint = masterFingerprint,
        )
    }
    LedgerSheet(
        action = action,
        onDismiss = onDismiss,
        onEvent = { event ->
            if (event is LedgerSheetEvent.SignPsbtSuccess) {
                onSignSuccess(event.signedPsbt)
                onDismiss()
            }
        },
    )
}

/**
 * Ledger health-check bottom sheet, shown inline by the host screen (signer info). Connects
 * over BLE/USB in-app and asks the device to sign the health-check message with the key at
 * [derivationPath], then verifies that signature against the stored [masterFingerprint] signer.
 *
 * [onResult] reports the verified outcome; the sheet dismisses either way, leaving the host to
 * display success/failure.
 */
@Composable
fun LedgerHealthCheckSheet(
    masterFingerprint: String,
    derivationPath: String,
    onDismiss: () -> Unit,
    onResult: (isSuccess: Boolean, errorMessage: String?) -> Unit,
) {
    val action = remember(masterFingerprint, derivationPath) {
        LedgerSheetAction.HealthCheck(
            masterFingerprint = masterFingerprint,
            derivationPath = derivationPath,
        )
    }
    LedgerSheet(
        action = action,
        onDismiss = onDismiss,
        onEvent = { event ->
            if (event is LedgerSheetEvent.HealthCheckResult) {
                onResult(event.isSuccess, event.errorMessage)
                onDismiss()
            }
        },
    )
}

/**
 * Shared body of every Ledger sheet: the device picker plus the transport plumbing that only
 * the UI layer can do (runtime permissions, the "turn on Bluetooth" prompt, error toasts).
 * The session itself lives in [LedgerSheetViewModel]; [onEvent] handles whatever is specific
 * to [action] and is responsible for dismissing on completion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LedgerSheet(
    action: LedgerSheetAction,
    onDismiss: () -> Unit,
    onEvent: (LedgerSheetEvent) -> Unit,
    viewModel: LedgerSheetViewModel = hiltViewModel(),
) {
    val activity = LocalActivity.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) viewModel.startScan() else viewModel.onPermissionsDenied()
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startScan()
        } else {
            viewModel.onBluetoothStillOff()
        }
    }

    val ensurePermissionThenScan = {
        if (viewModel.hasBlePermissions()) {
            viewModel.startScan()
        } else {
            permissionLauncher.launch(viewModel.requiredPermissions())
        }
    }

    // Subscribe before the first scan: events are one-shot, and startScan() can report
    // "Bluetooth is off" synchronously.
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is LedgerSheetEvent.WrongDevice -> activity?.let {
                    NCToastMessage(it).showError(it.getString(R.string.nc_ledger_wrong_device))
                }

                is LedgerSheetEvent.Error -> activity?.let {
                    NCToastMessage(it).showError(event.message)
                }

                is LedgerSheetEvent.RequestEnableBluetooth ->
                    runCatching {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }.onFailure { viewModel.onBluetoothStillOff() }

                else -> onEvent(event)
            }
        }
    }

    // Show and start scanning immediately.
    LaunchedEffect(Unit) { ensurePermissionThenScan() }

    // Tear the transport down when the sheet leaves composition, however it was dismissed.
    DisposableEffect(Unit) {
        onDispose { viewModel.closeSession() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        dragHandle = { },
    ) {
        LedgerSheetContent(
            isScanning = state.isScanning,
            devices = state.devices,
            selectedAddress = state.selectedDeviceId,
            statusText = state.statusText,
            connectButtonText = action.connectButtonText,
            onRescan = ensurePermissionThenScan,
            onRefreshUsb = { viewModel.refreshUsb() },
            onSelectDevice = { device -> viewModel.selectDevice(device.id) },
            onConnect = { viewModel.connect(action) },
        )
    }
}

/**
 * Sheet body: the shared device picker wrapping its content height so the sheet only takes
 * the space it needs, plus the action button.
 */
@Composable
private fun LedgerSheetContent(
    isScanning: Boolean = false,
    devices: List<LedgerDevice> = emptyList(),
    selectedAddress: String? = null,
    statusText: String = "",
    @StringRes connectButtonText: Int = R.string.nc_ledger_connect,
    onRescan: () -> Unit = {},
    onRefreshUsb: () -> Unit = {},
    onSelectDevice: (LedgerDevice) -> Unit = {},
    onConnect: () -> Unit = {},
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

@PreviewLightDark
@Composable
private fun LedgerSignTransactionSheetContentPreview() {
    NunchukTheme {
        LedgerSheetContent(
            devices = listOf(LedgerDevice("DE:F1:60:10:BA:AB", "Nano X E4F4", LedgerTransportKind.BLE)),
            selectedAddress = "DE:F1:60:10:BA:AB",
            connectButtonText = R.string.nc_ledger_sign_transaction,
        )
    }
}

@PreviewLightDark
@Composable
private fun LedgerHealthCheckSheetContentPreview() {
    NunchukTheme {
        LedgerSheetContent(
            devices = listOf(LedgerDevice("DE:F1:60:10:BA:AB", "Nano X E4F4", LedgerTransportKind.BLE)),
            selectedAddress = "DE:F1:60:10:BA:AB",
            connectButtonText = R.string.nc_ledger_sign_message,
        )
    }
}
