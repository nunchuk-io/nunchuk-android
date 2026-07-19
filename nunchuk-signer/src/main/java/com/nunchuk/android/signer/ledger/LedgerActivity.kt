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
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.ledger.LedgerActivity.Companion.EXTRA_RESULT_SUCCESS
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

    /** Which Confluence flow this activity is driving: add-key ("Get XPUB") or health check ("Sign message"). */
    private val action: String by lazy {
        intent.getStringExtra(EXTRA_ACTION) ?: ACTION_ADD_KEY
    }

    // Health-check target: fingerprint + derivation path of the key to verify (health check only).
    private val signerFingerprint: String by lazy {
        intent.getStringExtra(EXTRA_SIGNER_FINGERPRINT).orEmpty()
    }
    private val signerDerivationPath: String by lazy {
        intent.getStringExtra(EXTRA_SIGNER_DERIVATION_PATH).orEmpty()
    }

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

                LedgerRequest.SIGN_MESSAGE -> viewModel.healthCheck(
                    masterFingerprint = signerFingerprint,
                    derivationPath = signerDerivationPath,
                    message = HEALTH_CHECK_MESSAGE,
                    signature = result,
                )
            }
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

                is LedgerScanEvent.HealthCheckSuccess -> finishWithHealthCheckResult(success = true)

                is LedgerScanEvent.HealthCheckFailed ->
                    finishWithHealthCheckResult(success = false, errorMessage = event.message)

                is LedgerScanEvent.Error -> NCToastMessage(this).showError(event.message)
            }
        }

        setContent {
            NunchukTheme {
                val navController = rememberNavController()
                val state by viewModel.state.collectAsStateWithLifecycle()

                NavHost(
                    navController = navController,
                    // Health check targets an already-added key, so skip the "Add Ledger"
                    // intro and go straight to the device picker (it lists BLE + USB).
                    startDestination = if (action == ACTION_HEALTH_CHECK) {
                        LedgerDeviceScanRoute
                    } else {
                        LedgerIntroRoute
                    }
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
                            // When the scan screen is the start destination (health check),
                            // there's nothing to pop back to — close the flow instead.
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
                            controller.whenReady { startCommandForAction() }
                        },
                    )
                }

                if (state.isProcessing) {
                    NcLoadingDialog(onDismiss = {})
                }
            }
        }

        // Health check opens straight onto the device picker; kick off the scan once.
        if (savedInstanceState == null && action == ACTION_HEALTH_CHECK) {
            ensurePermissionThenScan()
        }
    }

    /** Kicks off the first native command once the transport is ready, per [action]. */
    private fun startCommandForAction() {
        when (action) {
            ACTION_HEALTH_CHECK -> controller.signMessage(signerDerivationPath, HEALTH_CHECK_MESSAGE)
            else -> controller.getMasterFingerprint()
        }
    }

    /**
     * Health check only: the signing + verification ran on this activity; hand the
     * success/failed outcome back to the caller (SignerInfo) to display, then close.
     */
    private fun finishWithHealthCheckResult(success: Boolean, errorMessage: String? = null) {
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(EXTRA_RESULT_SUCCESS, success)
                .putExtra(EXTRA_RESULT_ERROR, errorMessage)
        )
        finish()
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
        // Confluence "Sign message" health-check message. Signed on the device and verified
        // here with HealthCheckSingleSigner (same constant Trezor's health check uses).
        private const val HEALTH_CHECK_MESSAGE = "Run health check"

        private const val EXTRA_ACTION = "action"
        private const val EXTRA_SIGNER_FINGERPRINT = "signer_fingerprint"
        private const val EXTRA_SIGNER_DERIVATION_PATH = "signer_derivation_path"

        const val ACTION_ADD_KEY = "add_key"
        const val ACTION_HEALTH_CHECK = "health_check"

        // Health-check result extras (RESULT_OK): the outcome and an optional error message.
        const val EXTRA_RESULT_SUCCESS = "result_success"
        const val EXTRA_RESULT_ERROR = "result_error"

        /** Standalone "Add key" flow (Get XPUB). */
        fun buildIntent(activityContext: Context): Intent {
            return Intent(activityContext, LedgerActivity::class.java)
                .putExtra(EXTRA_ACTION, ACTION_ADD_KEY)
        }

        /**
         * Health-check flow: signs a health-check message with the key at [derivationPath],
         * verifies it against the signer [masterFingerprint], and returns the success/failed
         * result (see [EXTRA_RESULT_SUCCESS]) for the caller to display.
         */
        fun buildHealthCheckIntent(
            activityContext: Context,
            masterFingerprint: String,
            derivationPath: String,
        ): Intent {
            return Intent(activityContext, LedgerActivity::class.java)
                .putExtra(EXTRA_ACTION, ACTION_HEALTH_CHECK)
                .putExtra(EXTRA_SIGNER_FINGERPRINT, masterFingerprint)
                .putExtra(EXTRA_SIGNER_DERIVATION_PATH, derivationPath)
        }
    }
}
