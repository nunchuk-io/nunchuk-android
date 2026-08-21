package com.nunchuk.android.signer.ledger

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.ledger.LedgerBleController
import com.nunchuk.android.core.ledger.LedgerDevice
import com.nunchuk.android.core.ledger.LedgerRequest
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.LedgerUserInteraction
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.ResultExistingKey
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

/**
 * "Add key" flow for Ledger. Ledger talks to the device in-app over BLE/USB. This activity
 * owns the transport ([LedgerBleController], which needs an Android Context) and orchestrates
 * the Confluence "Get XPUB" step: connect -> getMasterFingerprint -> getExtendedPublicKey ->
 * create the signer.
 *
 * Two modes, same as [com.nunchuk.android.signer.trezor.TrezorActivity]:
 * - standalone (`isMembershipFlow = false`): the user picks wallet type / address type / account
 *   index, and the created signer opens its signer-info screen. This is also what the **free group
 *   wallet** uses (via SignerIntroActivity), since its wallet config is user-chosen — the key lands
 *   in the group slot through `PushEvent.LocalUserSignerAdded`.
 * - assisted-wallet / group-wallet membership flows (`isMembershipFlow = true`): those wallets only
 *   accept multisig keys (`isValidPathForAssistedWallet`), so the config is fixed to multisig /
 *   native segwit / [EXTRA_ACCOUNT_INDEX] and the select-wallet-type step is skipped. The created
 *   signer is returned to the caller via [GlobalResultKey.EXTRA_SIGNER], and the intro adds a
 *   desktop-app hand-off returned as [RESULT_ACTION_OPEN_DESKTOP_FLOW]. The on-chain timelock
 *   wallets hold two accounts of the same device per key slot, so they add the key twice — account
 *   0, then account 1 with [EXTRA_EXPECTED_XFP] set to the first key's fingerprint.
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

    private val isMembershipFlow: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_IS_MEMBERSHIP_FLOW, false)
    }

    /** Account to read the xpub from; only the membership flows set it (they skip the config step). */
    private val accountIndex: Int by lazy {
        intent.getIntExtra(EXTRA_ACCOUNT_INDEX, 0)
    }

    /** Set when the key has to come off a specific device — the other account of an on-chain pair,
     *  or, when verifying a seed-phrase backup, the key that backup belongs to. */
    private val expectedXfp: String by lazy {
        intent.getStringExtra(EXTRA_EXPECTED_XFP).orEmpty()
    }

    /**
     * Verifying a seed-phrase backup: the key already exists and the user restored its seed onto
     * this device, so connecting is the whole check. Read the fingerprint, hand it back and let
     * the caller mark the key verified — no xpub, no key created.
     */
    private val isVerifyXfpOnly: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_VERIFY_XFP_ONLY, false)
    }

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
                    // Both accounts of an on-chain key slot have to come off one device, otherwise
                    // the wallet is built from a key the user never meant to add. In verify mode
                    // the same check is what proves the restored seed is the inheritance key.
                    if (expectedXfp.isNotEmpty() && !result.equals(expectedXfp, ignoreCase = true)) {
                        viewModel.onError(xfpMismatchMessage(result))
                        return
                    }
                    if (isVerifyXfpOnly) {
                        setResult(
                            RESULT_OK,
                            Intent().apply {
                                putExtra(EXTRA_VERIFIED_XFP, result.lowercase(Locale.getDefault()))
                            }
                        )
                        finish()
                        return
                    }
                    masterFingerprint = result
                    val config = viewModel.state.value
                    controller.getExtendedPublicKey(config.walletType, config.addressType, config.accountIndex)
                }

                LedgerRequest.XPUB -> {
                    viewModel.onXpubReceived(
                        masterFingerprint = masterFingerprint,
                        xpub = result,
                    )
                }

                // Only the inline sheets drive sign-message / register / sign-psbt / show-address.
                LedgerRequest.SIGN_MESSAGE,
                LedgerRequest.REGISTER_WALLET,
                LedgerRequest.SIGN_PSBT,
                LedgerRequest.GET_WALLET_ADDRESS -> Unit
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

        viewModel.setMembershipFlow(isMembershipFlow)
        if (isMembershipFlow) {
            // Assisted/group membership wallets take multisig keys only, so the config is fixed here
            // instead of on the (skipped) select-wallet-type step. Free group wallet is not this mode.
            viewModel.setWalletConfig(
                walletType = WalletType.MULTI_SIG,
                addressType = AddressType.NATIVE_SEGWIT,
                index = accountIndex,
            )
        }

        setContent {
            NunchukTheme {
                val navController = rememberNavController()
                val state by viewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.event.collect { event ->
                        when (event) {
                            LedgerScanEvent.NavigateToSetKeyName -> {
                                if (navController.currentDestination?.route != ledgerSetKeyNameRoute) {
                                    navController.navigateToLedgerSetKeyName()
                                }
                            }

                            is LedgerScanEvent.OpenSignerInfo -> {
                                val signer = event.signer
                                if (isMembershipFlow) {
                                    setResult(
                                        RESULT_OK,
                                        Intent().apply {
                                            putExtra(GlobalResultKey.EXTRA_SIGNER, signer)
                                        }
                                    )
                                } else {
                                    navigator.openSignerInfoScreen(
                                        activityContext = this@LedgerActivity,
                                        isMasterSigner = signer.hasMasterSigner,
                                        id = signer.masterFingerprint,
                                        masterFingerprint = signer.masterFingerprint,
                                        name = signer.name,
                                        type = signer.type,
                                        derivationPath = signer.derivationPath,
                                        justAdded = true,
                                    )
                                }
                                finish()
                            }

                            is LedgerScanEvent.Error ->
                                NCToastMessage(this@LedgerActivity).showError(event.message)
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = LedgerIntroRoute,
                ) {
                    ledgerIntro(
                        onBack = { finish() },
                        onAddViaBluetooth = { navController.navigateToLedgerInstruction(isUsb = false) },
                        onAddViaUsb = { navController.navigateToLedgerInstruction(isUsb = true) },
                        onAddViaDesktop = {
                            setResult(
                                RESULT_OK,
                                Intent().apply {
                                    putExtra(EXTRA_RESULT_ACTION, RESULT_ACTION_OPEN_DESKTOP_FLOW)
                                }
                            )
                            finish()
                        },
                        isAddViaUsbEnabled = true,
                        // Verifying a backup means reading this device; the desktop app can't
                        // stand in for it.
                        isAddViaDesktopEnabled = isMembershipFlow && !isVerifyXfpOnly,
                    )
                    ledgerSelectWalletType(
                        onBack = { navController.popBackStack() },
                        onContinue = { isUsb, isSingleSig, addressType, accountIndex ->
                            viewModel.setWalletConfig(
                                walletType = if (isSingleSig) WalletType.SINGLE_SIG else WalletType.MULTI_SIG,
                                addressType = addressType,
                                index = accountIndex,
                            )
                            navController.navigateToLedgerDeviceScan()
                            if (isUsb) {
                                // USB needs no Bluetooth permission — just list attached devices.
                                controller.refreshUsb()
                            } else {
                                ensurePermissionThenScan()
                            }
                        },
                    )
                    ledgerInstruction(
                        onBack = { navController.popBackStack() },
                        // Add-key-to-wallet has the config fixed (multisig / native segwit /
                        // account index from the caller), so it skips the select-wallet-type step
                        // and connects right away.
                        onContinue = { isUsb ->
                            if (isMembershipFlow) {
                                navController.navigateToLedgerDeviceScan()
                                if (isUsb) {
                                    // USB needs no Bluetooth permission — just list attached devices.
                                    controller.refreshUsb()
                                } else {
                                    ensurePermissionThenScan()
                                }
                            } else {
                                navController.navigateToLedgerSelectWalletType(isUsb = isUsb)
                            }
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
                    // Standalone add-key only: membership flows auto-name the key and never
                    // reach this step.
                    ledgerSetKeyName(
                        defaultName = { state.defaultSignerName },
                        onBack = { navController.popBackStack() },
                        onContinue = { name -> viewModel.createLedgerSigner(name) },
                    )
                }

                val pendingSigner = state.pendingSigner
                if (state.existingKeyType != null && pendingSigner != null) {
                    val fingerprint = pendingSigner.masterFingerprint.uppercase(Locale.getDefault())
                    val messageRes = if (state.existingKeyType == ResultExistingKey.Software) {
                        com.nunchuk.android.core.R.string.nc_existing_key_is_software_key_delete_key
                    } else {
                        com.nunchuk.android.core.R.string.nc_existing_key_change_key_type
                    }
                    NcConfirmationDialog(
                        title = stringResource(id = R.string.nc_info),
                        message = stringResource(id = messageRes, fingerprint),
                        positiveButtonText = stringResource(id = com.nunchuk.android.core.R.string.nc_text_yes),
                        negativeButtonText = stringResource(id = com.nunchuk.android.core.R.string.nc_text_no),
                        onPositiveClick = { viewModel.confirmExistingKeyDialog() },
                        onDismiss = { viewModel.dismissExistingKeyDialog() },
                    )
                }

                if (state.isProcessing) {
                    NcLoadingDialog(onDismiss = {})
                }
            }
        }
    }

    private fun xfpMismatchMessage(actualXfp: String): String = if (isVerifyXfpOnly) {
        getString(
            R.string.nc_verify_key_xfp_not_match,
            actualXfp.uppercase(Locale.getDefault()),
            expectedXfp.uppercase(Locale.getDefault()),
        )
    } else {
        getString(R.string.nc_added_key_xfp_mismatch)
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
        const val EXTRA_IS_MEMBERSHIP_FLOW = "extra_is_membership_flow"
        const val EXTRA_ACCOUNT_INDEX = "extra_account_index"
        const val EXTRA_EXPECTED_XFP = "extra_expected_xfp"
        const val EXTRA_VERIFY_XFP_ONLY = "extra_verify_xfp_only"
        const val EXTRA_RESULT_ACTION = "extra_result_action"

        /** Lowercased fingerprint of the connected device, returned by [verifyXfpOnly]. */
        const val EXTRA_VERIFIED_XFP = "extra_verified_xfp"

        /** The user chose to claim the key from the desktop app instead of pairing here. */
        const val RESULT_ACTION_OPEN_DESKTOP_FLOW = "result_action_open_desktop_flow"

        /**
         * "Add key" flow (Get XPUB). [accountIndex] and [expectedXfp] only apply to
         * [isMembershipFlow], which skips the screen where the user would pick the account.
         *
         * [verifyXfpOnly] turns this into a "which device is this?" round trip used by the
         * seed-phrase-backup verification: no key is created and [EXTRA_VERIFIED_XFP] comes back on
         * RESULT_OK. Pass [expectedXfp] with it — a device reporting anything else is turned away
         * here rather than by the caller.
         */
        fun buildIntent(
            activityContext: Context,
            isMembershipFlow: Boolean = false,
            accountIndex: Int = 0,
            expectedXfp: String = "",
            verifyXfpOnly: Boolean = false,
        ): Intent = Intent(activityContext, LedgerActivity::class.java).apply {
            putExtra(EXTRA_IS_MEMBERSHIP_FLOW, isMembershipFlow)
            putExtra(EXTRA_ACCOUNT_INDEX, accountIndex)
            putExtra(EXTRA_EXPECTED_XFP, expectedXfp)
            putExtra(EXTRA_VERIFY_XFP_ONLY, verifyXfpOnly)
        }
    }
}
