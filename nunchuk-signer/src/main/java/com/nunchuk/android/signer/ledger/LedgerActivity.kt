package com.nunchuk.android.signer.ledger

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.ledger.LedgerBleController
import com.nunchuk.android.core.ledger.LedgerDevice
import com.nunchuk.android.core.ledger.LedgerRequest
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.LedgerUserInteraction
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Standalone "Add key" flow for Ledger (not add-key-to-wallet). Ledger talks to the
 * device in-app over BLE. This activity owns the BLE transport ([LedgerBleController],
 * which needs an Android Context) and orchestrates the Confluence "Get XPUB" step:
 * connect -> getMasterFingerprint -> getExtendedPublicKey -> create the signer.
 *
 * (Sign transaction and health check are hosted inline on their own screens as
 * [com.nunchuk.android.core.ledger.LedgerSignTransactionSheet] /
 * [com.nunchuk.android.core.ledger.LedgerHealthCheckSheet].)
 */
@AndroidEntryPoint
class LedgerActivity : BaseComposeActivity() {

    @Inject
    lateinit var nativeSdk: NunchukNativeSdk

    private val viewModel: LedgerViewModel by viewModels()

    private val controller: LedgerBleController by lazy {
        LedgerBleController(this, nativeSdk, bleListener)
    }
    private var masterFingerprint: String = ""

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            startScan()
        } else {
            NCToastMessage(this).showError(getString(R.string.nc_ledger_bluetooth_permission_required))
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startScan()
        } else {
            viewModel.setScanning(false)
            viewModel.setStatus(getString(R.string.nc_ledger_bluetooth_off))
        }
    }

    private val bleListener = object : LedgerBleController.Listener {
        override fun onScanResults(devices: List<LedgerDevice>) = viewModel.setDevices(devices)

        override fun onScanFinished(found: Int) {
            viewModel.setScanning(false)
            if (found == 0) viewModel.setStatus(getString(R.string.nc_ledger_no_devices_found))
        }

        override fun onConnecting(device: LedgerDevice) {
            viewModel.setScanning(false)
            viewModel.setStatus(getString(R.string.nc_ledger_connecting, device.name))
        }

        override fun onInteraction(interaction: LedgerUserInteraction) {
            viewModel.setStatus(interactionText(interaction))
        }

        override fun onCommandComplete(request: LedgerRequest, result: String) {
            when (request) {
                LedgerRequest.MASTER_FINGERPRINT -> {
                    masterFingerprint = result
                    val config = viewModel.state.value
                    controller.getExtendedPublicKey(config.walletType, config.addressType, config.accountIndex)
                }

                LedgerRequest.XPUB -> {
                    val config = viewModel.state.value
                    viewModel.createSigner(
                        name = getString(R.string.nc_ledger),
                        masterFingerprint = masterFingerprint,
                        xpub = result,
                        walletType = config.walletType,
                        addressType = config.addressType,
                        index = config.accountIndex,
                    )
                }

                // Only the inline sheets drive sign-message / register / sign-psbt.
                LedgerRequest.SIGN_MESSAGE,
                LedgerRequest.REGISTER_WALLET,
                LedgerRequest.SIGN_PSBT -> Unit
            }
        }

        override fun onCommandFailed(request: LedgerRequest, statusWord: Int, message: String) {
            // Add-key doesn't branch on the status word; surface as a plain error.
            viewModel.setScanning(false)
            viewModel.onError(message)
        }

        override fun onError(message: String) {
            viewModel.setScanning(false)
            viewModel.onError(message)
        }

        override fun onBluetoothDisabled() {
            viewModel.setScanning(false)
            runCatching {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }.onFailure {
                viewModel.setStatus(getString(R.string.nc_ledger_bluetooth_off))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        flowObserver(viewModel.event) { event ->
            when (event) {
                is LedgerScanEvent.OpenSignerInfo -> {
                    val signer = event.signer
                    navigator.openSignerInfoScreen(
                        activityContext = this,
                        isMasterSigner = signer.hasMasterSigner,
                        id = signer.masterFingerprint,
                        masterFingerprint = signer.masterFingerprint,
                        name = signer.name,
                        type = signer.type,
                        derivationPath = signer.derivationPath,
                        justAdded = true,
                    )
                    finish()
                }

                is LedgerScanEvent.Error -> NCToastMessage(this).showError(event.message)
            }
        }

        setContent {
            NunchukTheme {
                val navController = rememberNavController()
                val state by viewModel.state.collectAsStateWithLifecycle()

                NavHost(
                    navController = navController,
                    startDestination = LedgerIntroRoute,
                ) {
                    ledgerIntro(
                        onBack = { finish() },
                        // Pick the wallet/address type first, then continue to the transport-specific step.
                        onAddViaBluetooth = { navController.navigateToLedgerSelectWalletType(isUsb = false) },
                        onAddViaUsb = { navController.navigateToLedgerSelectWalletType(isUsb = true) },
                        isAddViaUsbEnabled = true
                    )
                    ledgerSelectWalletType(
                        onBack = { navController.popBackStack() },
                        onContinue = { isUsb, isSingleSig, addressType, accountIndex ->
                            viewModel.setWalletConfig(
                                walletType = if (isSingleSig) WalletType.SINGLE_SIG else WalletType.MULTI_SIG,
                                addressType = addressType,
                                index = accountIndex,
                            )
                            if (isUsb) {
                                // USB needs no Bluetooth permission — just list attached devices.
                                navController.navigateToLedgerDeviceScan()
                                controller.refreshUsb()
                            } else {
                                navController.navigateToLedgerInstruction()
                            }
                        },
                    )
                    ledgerInstruction(
                        onBack = { navController.popBackStack() },
                        onContinue = {
                            navController.navigateToLedgerDeviceScan()
                            ensurePermissionThenScan()
                        }
                    )
                    ledgerDeviceScan(
                        onBack = {
                            controller.close()
                            if (!navController.popBackStack()) finish()
                        },
                        isScanning = { state.isScanning },
                        devices = { state.devices },
                        selectedAddress = { state.selectedAddress },
                        statusText = { state.statusText },
                        onRescan = { ensurePermissionThenScan() },
                        onRefreshUsb = { controller.refreshUsb() },
                        onSelectDevice = { device -> viewModel.selectDevice(device.id) },
                        onConnect = {
                            val device = state.selectedDevice ?: return@ledgerDeviceScan
                            controller.connect(device)
                            controller.whenReady { controller.getMasterFingerprint() }
                        },
                    )
                }

                if (state.isProcessing) {
                    NcLoadingDialog(onDismiss = {})
                }
            }
        }
    }

    private fun ensurePermissionThenScan() {
        if (controller.hasBlePermissions()) {
            startScan()
        } else {
            permissionLauncher.launch(controller.requiredPermissions())
        }
    }

    private fun startScan() {
        viewModel.setStatus("")
        viewModel.setScanning(true)
        controller.startScan()
    }

    private fun interactionText(interaction: LedgerUserInteraction): String = when (interaction) {
        LedgerUserInteraction.UNLOCK_DEVICE -> getString(R.string.nc_ledger_unlock_device)
        LedgerUserInteraction.CONFIRM_OPEN_APP -> getString(R.string.nc_ledger_confirm_open_app)
        LedgerUserInteraction.VERIFY_ADDRESS -> getString(R.string.nc_ledger_verify_address)
        LedgerUserInteraction.REGISTER_WALLET -> getString(R.string.nc_ledger_register_wallet)
        LedgerUserInteraction.SIGN_MESSAGE -> getString(R.string.nc_ledger_confirm_sign_message)
        LedgerUserInteraction.SIGN_TRANSACTION -> getString(R.string.nc_ledger_confirm_sign_transaction)
        LedgerUserInteraction.NONE -> getString(R.string.nc_ledger_communicating)
    }

    override fun onDestroy() {
        controller.close()
        super.onDestroy()
    }

    companion object {
        /** Standalone "Add key" flow (Get XPUB). */
        fun buildIntent(activityContext: Context): Intent =
            Intent(activityContext, LedgerActivity::class.java)
    }
}
