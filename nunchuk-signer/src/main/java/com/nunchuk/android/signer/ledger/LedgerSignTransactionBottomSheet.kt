package com.nunchuk.android.signer.ledger

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.nav.SignerNavigator
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.LedgerUserInteraction
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Ledger "Sign transaction" as a bottom sheet, shown inline in the transaction detail screen.
 * Owns the in-app BLE/USB transport ([LedgerBleController]) and reuses the shared device
 * picker ([LedgerDeviceScanScreen]); the sign logic lives in the injectable
 * [com.nunchuk.android.core.domain.utils.LedgerTransactionSigner] coordinator, driven via
 * [LedgerControllerExecutor]. On success the signed PSBT is already imported, and a fragment
 * result is published under [SignerNavigator.LEDGER_SIGN_TX_REQUEST_KEY] so the host refreshes.
 */
@AndroidEntryPoint
class LedgerSignTransactionBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var nativeSdk: NunchukNativeSdk

    private val viewModel: LedgerViewModel by viewModels()

    private val controller: LedgerBleController by lazy {
        LedgerBleController(requireContext(), nativeSdk, bleListener)
    }
    private val executor: LedgerControllerExecutor by lazy {
        LedgerControllerExecutor(controller)
    }

    private val walletId: String by lazy { requireArguments().getString(EXTRA_WALLET_ID).orEmpty() }
    private val txId: String by lazy { requireArguments().getString(EXTRA_TX_ID).orEmpty() }
    private val xfp: String by lazy { requireArguments().getString(EXTRA_XFP).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.nunchuk.android.core.R.style.NCBottomSheetDialogStyle)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            startScan()
        } else {
            NCToastMessage(requireActivity())
                .showError(getString(R.string.nc_ledger_bluetooth_permission_required))
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
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

        // Sign runs entirely through the coordinator; hand every command result to the executor.
        override fun onCommandComplete(request: LedgerRequest, result: String) {
            executor.deliverComplete(result)
        }

        override fun onCommandFailed(request: LedgerRequest, statusWord: Int, message: String) {
            executor.deliverFailure(statusWord, message)
        }

        override fun onError(message: String) {
            // A transport error mid-command fails the in-flight sign; otherwise it's a scan error.
            if (executor.isAwaiting) {
                executor.deliverFailure(statusWord = 0, message = message)
            } else {
                viewModel.setScanning(false)
                viewModel.onError(message)
            }
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NunchukTheme {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    LedgerDeviceScanSheet(
                        isScanning = state.isScanning,
                        devices = state.devices,
                        selectedAddress = state.selectedAddress,
                        statusText = state.statusText,
                        onRescan = { ensurePermissionThenScan() },
                        onRefreshUsb = { controller.refreshUsb() },
                        onSelectDevice = { device -> viewModel.selectDevice(device.id) },
                        onConnect = {
                            // Sign is a multi-step device conversation; don't reconnect mid-flow.
                            if (state.isSigning) return@LedgerDeviceScanSheet
                            val device = state.selectedDevice ?: return@LedgerDeviceScanSheet
                            controller.connect(device)
                            controller.whenReady { startSign() }
                        },
                        connectButtonText = R.string.nc_ledger_sign_transaction,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flowObserver(viewModel.event) { event ->
            when (event) {
                is LedgerScanEvent.SignTransactionSuccess -> {
                    setFragmentResult(
                        SignerNavigator.LEDGER_SIGN_TX_REQUEST_KEY,
                        bundleOf(SignerNavigator.LEDGER_SIGN_TX_SUCCESS to true),
                    )
                    dismissAllowingStateLoss()
                }

                is LedgerScanEvent.SignTransactionWrongDevice ->
                    NCToastMessage(requireActivity())
                        .showError(getString(R.string.nc_ledger_wrong_device))

                is LedgerScanEvent.Error -> NCToastMessage(requireActivity()).showError(event.message)

                // Add-key / health-check events never fire in this flow.
                else -> Unit
            }
        }

        // Show and start scanning immediately.
        ensurePermissionThenScan()
    }

    private fun startSign() {
        viewModel.signTransaction(
            executor = executor,
            walletId = walletId,
            txId = txId,
            expectedXfp = xfp,
        )
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
        const val TAG = "LedgerSignTransactionBottomSheet"
        private const val EXTRA_WALLET_ID = "wallet_id"
        private const val EXTRA_TX_ID = "tx_id"
        private const val EXTRA_XFP = "xfp"

        fun show(
            fragmentManager: FragmentManager,
            walletId: String,
            txId: String,
            masterFingerprint: String,
        ) = LedgerSignTransactionBottomSheet().apply {
            arguments = bundleOf(
                EXTRA_WALLET_ID to walletId,
                EXTRA_TX_ID to txId,
                EXTRA_XFP to masterFingerprint,
            )
            show(fragmentManager, TAG)
        }
    }
}
