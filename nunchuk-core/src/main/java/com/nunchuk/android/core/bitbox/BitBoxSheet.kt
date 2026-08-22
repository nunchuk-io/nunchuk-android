package com.nunchuk.android.core.bitbox

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import com.nunchuk.android.core.hardware.HardwareConnectButton
import com.nunchuk.android.core.hardware.HardwareDeviceScanBody
import com.nunchuk.android.core.hardware.KeepScreenOn
import com.nunchuk.android.core.hardware.HardwareTransportKind
import com.nunchuk.android.core.hardware.HardwareVerifyAddressBox
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.widget.NCToastMessage

/**
 * BitBox health-check bottom sheet, shown inline by the host screen (signer info). Connects over
 * BLE/USB in-app, initializes the session, then asks the device to sign the health-check message
 * at the signer's BitBox signing path and verifies that signature against the stored
 * [masterFingerprint] + [derivationPath] signer.
 *
 * The BitBox counterpart of [com.nunchuk.android.core.ledger.LedgerHealthCheckSheet].
 *
 * [onResult] reports the verified outcome; the sheet dismisses either way, leaving the host to
 * display success/failure. A device that never got as far as signing — not set up, wrong
 * firmware, failed attestation, pairing declined — reports nothing and leaves the sheet open to
 * retry.
 */
@Composable
fun BitBoxHealthCheckSheet(
    masterFingerprint: String,
    derivationPath: String,
    onDismiss: () -> Unit,
    onResult: (isSuccess: Boolean, errorMessage: String?) -> Unit,
) {
    val action = remember(masterFingerprint, derivationPath) {
        BitBoxSheetAction.HealthCheck(
            masterFingerprint = masterFingerprint,
            derivationPath = derivationPath,
        )
    }
    BitBoxSheet(
        action = action,
        onDismiss = onDismiss,
        onEvent = { event ->
            if (event is BitBoxSheetEvent.HealthCheckResult) {
                onResult(event.isSuccess, event.errorMessage)
                onDismiss()
            }
        },
    )
}

/**
 * BitBox "Sign transaction" bottom sheet, shown inline by the host screen (transaction detail).
 * Connects over BLE/USB in-app, verifies the device is [masterFingerprint], then
 * registers-if-needed + signs [txId] in [walletId] and imports the signed PSBT.
 *
 * [onSignSuccess] fires once the signed PSBT is imported so the host can refresh.
 */
@Composable
fun BitBoxSignTransactionSheet(
    walletId: String,
    txId: String,
    masterFingerprint: String,
    onDismiss: () -> Unit,
    onSignSuccess: () -> Unit,
) {
    val action = remember(walletId, txId, masterFingerprint) {
        BitBoxSheetAction.SignTransaction(
            walletId = walletId,
            txId = txId,
            masterFingerprint = masterFingerprint,
        )
    }
    BitBoxSheet(
        action = action,
        onDismiss = onDismiss,
        onEvent = { event ->
            if (event is BitBoxSheetEvent.SignTransactionSuccess) {
                onSignSuccess()
                onDismiss()
            }
        },
    )
}

/**
 * BitBox "Sign transaction" bottom sheet for a dummy transaction, shown inline by the host screen
 * (dummy transaction details, membership sign-message check). Connects over BLE/USB in-app,
 * verifies the device is [masterFingerprint], registers [walletId]'s policy if the device doesn't
 * already have it, and signs [psbt] — handing the signed PSBT to [onSignSuccess] rather than
 * importing it, so the host can extract the dummy transaction signature from it.
 */
@Composable
fun BitBoxSignPsbtSheet(
    walletId: String,
    psbt: String,
    masterFingerprint: String,
    onDismiss: () -> Unit,
    onSignSuccess: (signedPsbt: String) -> Unit,
) {
    val action = remember(walletId, psbt, masterFingerprint) {
        BitBoxSheetAction.SignPsbt(
            walletId = walletId,
            psbt = psbt,
            masterFingerprint = masterFingerprint,
        )
    }
    BitBoxSheet(
        action = action,
        onDismiss = onDismiss,
        onEvent = { event ->
            if (event is BitBoxSheetEvent.SignPsbtSuccess) {
                onSignSuccess(event.signedPsbt)
                onDismiss()
            }
        },
    )
}

/**
 * [BitBoxSignPsbtSheet] for a [wallet] that isn't stored locally, so it can't be looked up by id:
 * the sign-in dummy transaction, whose wallet is parsed from the BSMS the user pasted.
 */
@Composable
fun BitBoxSignPsbtSheet(
    wallet: Wallet,
    psbt: String,
    masterFingerprint: String,
    onDismiss: () -> Unit,
    onSignSuccess: (signedPsbt: String) -> Unit,
) {
    val action = remember(wallet, psbt, masterFingerprint) {
        BitBoxSheetAction.SignPsbtWithWallet(
            wallet = wallet,
            psbt = psbt,
            masterFingerprint = masterFingerprint,
        )
    }
    BitBoxSheet(
        action = action,
        onDismiss = onDismiss,
        onEvent = { event ->
            if (event is BitBoxSheetEvent.SignPsbtSuccess) {
                onSignSuccess(event.signedPsbt)
                onDismiss()
            }
        },
    )
}

/**
 * BitBox "Show address on device" bottom sheet, shown inline by the host screen (receive
 * addresses). Connects over BLE/USB in-app, registers [walletId] on the device if it isn't
 * already, then asks the device to display [address] and compares what it derived.
 *
 * [onResult] reports whether the device showed the same address; the sheet dismisses either way,
 * leaving the host to display success/failure.
 */
@Composable
fun BitBoxVerifyAddressSheet(
    walletId: String,
    address: String,
    onDismiss: () -> Unit,
    onResult: (isMatch: Boolean) -> Unit,
) {
    val action = remember(walletId, address) {
        BitBoxSheetAction.VerifyAddress(walletId = walletId, address = address)
    }
    BitBoxSheet(
        action = action,
        onDismiss = onDismiss,
        onEvent = { event ->
            if (event is BitBoxSheetEvent.VerifyAddressResult) {
                onResult(event.isMatch)
                onDismiss()
            }
        },
    )
}

/**
 * Shared body of every BitBox sheet: the device picker (or the pairing code, while that answer
 * is outstanding) plus the transport plumbing that only the UI layer can do — runtime
 * permissions, the "turn on Bluetooth" prompt, error toasts.
 *
 * The session itself lives in [BitBoxSheetViewModel]; [onEvent] handles whatever is specific to
 * [action] and is responsible for dismissing on completion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BitBoxSheet(
    action: BitBoxSheetAction,
    onDismiss: () -> Unit,
    onEvent: (BitBoxSheetEvent) -> Unit,
    viewModel: BitBoxSheetViewModel = hiltViewModel(),
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
                is BitBoxSheetEvent.WrongDevice -> activity?.let {
                    NCToastMessage(it).showError(it.getString(R.string.nc_bitbox_wrong_device))
                }

                is BitBoxSheetEvent.Error -> activity?.let {
                    NCToastMessage(it).showError(event.message)
                }

                is BitBoxSheetEvent.RequestEnableBluetooth ->
                    runCatching {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }.onFailure { viewModel.onBluetoothStillOff() }

                else -> onEvent(event)
            }
        }
    }

    // Show and start scanning immediately.
    LaunchedEffect(Unit) { ensurePermissionThenScan() }

    // Minutes of reading and approving on the device itself, with nothing to touch
    // here; a display timeout under that drops the sign result.
    KeepScreenOn()

    // Tear the transport down when the sheet leaves composition, however it was dismissed.
    DisposableEffect(Unit) {
        onDispose { viewModel.closeSession() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Open fully rather than half-height: the picker is a short, self-contained flow, and a
        // partially expanded sheet can cut off the action button until the user drags it up.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        dragHandle = { },
    ) {
        BitBoxSheetContent(
            isScanning = state.isScanning,
            isBusy = state.isBusy,
            devices = state.devices,
            selectedAddress = state.selectedDeviceId,
            statusText = state.statusText,
            pairingCode = state.pairingCode,
            // Shown so the user has something to compare the device screen against.
            verifyAddress = (action as? BitBoxSheetAction.VerifyAddress)?.address,
            connectButtonText = action.connectButtonText,
            onRescan = ensurePermissionThenScan,
            onRefreshUsb = { viewModel.refreshUsb() },
            onSelectDevice = { device -> viewModel.selectDevice(device.id) },
            onConnect = { viewModel.connect(action) },
            onConfirmPairing = { accepted -> viewModel.confirmPairing(accepted) },
        )
    }
}

/**
 * Sheet body, wrapping its content height so the sheet only takes the space it needs.
 *
 * While [pairingCode] is set the picker is replaced rather than stacked on: the device is asking
 * the user the same question at the same time, and the two answer buttons are the only thing
 * that step is waiting on.
 */
@Composable
private fun BitBoxSheetContent(
    isScanning: Boolean = false,
    isBusy: Boolean = false,
    devices: List<BitBoxDevice> = emptyList(),
    selectedAddress: String? = null,
    statusText: String = "",
    pairingCode: String? = null,
    verifyAddress: String? = null,
    @StringRes connectButtonText: Int = R.string.nc_ledger_connect,
    onRescan: () -> Unit = {},
    onRefreshUsb: () -> Unit = {},
    onSelectDevice: (BitBoxDevice) -> Unit = {},
    onConnect: () -> Unit = {},
    onConfirmPairing: (accepted: Boolean) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 12.dp),
    ) {
        if (pairingCode != null) {
            BitBoxPairingCodeBody(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                pairingCode = pairingCode,
            )
            Column(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onConfirmPairing(true) },
                ) {
                    Text(text = stringResource(id = R.string.nc_bitbox_codes_match))
                }
                NcOutlineButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onConfirmPairing(false) },
                ) {
                    Text(text = stringResource(id = R.string.nc_bitbox_codes_do_not_match))
                }
            }
            return@Column
        }

        HardwareDeviceScanBody(
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
            titleRes = R.string.nc_bitbox_connect_a_device,
            subtitleRes = R.string.nc_bitbox_connect_a_device_desc,
            usbEmptyRes = R.string.nc_bitbox_usb_empty,
            deviceIconRes = R.drawable.ic_bitbox_hardware,
        )
        if (verifyAddress != null) {
            HardwareVerifyAddressBox(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                address = verifyAddress,
                labelRes = R.string.nc_bitbox_check_this_address,
            )
        }
        HardwareConnectButton(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            enabled = selectedAddress != null,
            connectButtonText = connectButtonText,
            isBusy = isBusy,
            onConnect = onConnect,
        )
    }
}

@PreviewLightDark
@Composable
private fun BitBoxHealthCheckSheetContentPreview() {
    NunchukTheme {
        BitBoxSheetContent(
            devices = listOf(
                BitBoxDevice("DE:F1:60:10:BA:AB", "BitBox02 Nova", HardwareTransportKind.BLE)
            ),
            selectedAddress = "DE:F1:60:10:BA:AB",
            connectButtonText = R.string.nc_ledger_sign_message,
        )
    }
}

@PreviewLightDark
@Composable
private fun BitBoxSheetPairingContentPreview() {
    NunchukTheme {
        BitBoxSheetContent(pairingCode = "VQ237 XAFZK")
    }
}
