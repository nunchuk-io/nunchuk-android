package com.nunchuk.android.signer.bitbox

import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.bitbox.BitBoxController
import com.nunchuk.android.core.bitbox.BitBoxDevice
import com.nunchuk.android.core.bitbox.BitBoxRequest
import com.nunchuk.android.core.bitbox.isBitBoxFirmwareTooNew
import com.nunchuk.android.core.bitbox.isOperationError
import com.nunchuk.android.core.bitbox.isSessionLost
import com.nunchuk.android.core.bitbox.isUserCancellation
import com.nunchuk.android.core.bitbox.statusText
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.BitBoxErrorCode
import com.nunchuk.android.type.BitBoxUserInteraction
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.ResultExistingKey
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

/**
 * "Add key" flow for BitBox02, the BitBox counterpart of
 * [com.nunchuk.android.signer.ledger.LedgerActivity]. The device is driven in-app over BLE/USB,
 * so this activity owns the transport ([BitBoxController], which needs an Android Context) and
 * runs the Confluence "Get XPUB" step: connect → initialize → getMasterFingerprint →
 * getExtendedPublicKey → create the signer.
 *
 * Two differences from Ledger, both from the BitBox protocol:
 * - **initialize comes first.** Every session starts with `initialize()`, whose result says
 *   whether the device is genuine, whether its firmware is usable, and whether it has been set
 *   up. Anything short of ready ends the flow on a hand-off screen (05B / 05C) — Nunchuk no
 *   longer carries device setup, per the 2026-08-21 design revision.
 * - **pairing may need confirming.** A first-time pairing surfaces a code to compare with the
 *   device; pairing data is stored per Nunchuk account/chain, so a known device skips it.
 *
 * Modes match Ledger: standalone (user picks wallet config, signer opens its info screen) and
 * membership (`isMembershipFlow`, config fixed to multisig / native segwit / [EXTRA_ACCOUNT_INDEX],
 * signer returned via [GlobalResultKey.EXTRA_SIGNER]).
 */
@AndroidEntryPoint
class BitBoxActivity : BaseComposeActivity() {

    @Inject
    lateinit var nativeSdk: NunchukNativeSdk

    private val viewModel: BitBoxViewModel by viewModels()

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

    private val controller: BitBoxController by lazy {
        BitBoxController(this, nativeSdk, deviceListener)
    }

    /**
     * Whether this connection has already had its Noise session rebuilt once. Reset on connect,
     * so a device that keeps losing its session reports instead of looping.
     */
    private var sessionRebuilt = false

    /**
     * Firmware version from the last successful `initialize()`. An `UNSUPPORTED_FIRMWARE` failure
     * carries no version of its own, so this is what lets the message say which end of the
     * supported window the device is on.
     */
    private var lastFirmwareVersion: String = ""

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            startScan()
        } else {
            NCToastMessage(this).showError(getString(R.string.nc_bitbox_bluetooth_permission_required))
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startScan()
        } else {
            viewModel.setScanning(false)
            viewModel.setStatus(getString(R.string.nc_bitbox_bluetooth_off))
        }
    }

    private val deviceListener = object : BitBoxController.Listener {
        override fun onScanResults(devices: List<BitBoxDevice>) = viewModel.setDevices(devices)

        override fun onScanFinished(found: Int) {
            viewModel.setScanning(false)
            if (found == 0) viewModel.setStatus(getString(R.string.nc_bitbox_no_devices_found))
        }

        override fun onConnecting(device: BitBoxDevice) {
            viewModel.setScanning(false)
            viewModel.setStatus(getString(R.string.nc_ledger_connecting, device.name))
        }

        override fun onInteraction(interaction: BitBoxUserInteraction) {
            viewModel.setStatus(interaction.statusText(this@BitBoxActivity))
        }

        override fun onPairingCode(code: String) {
            viewModel.onPairingCode(code)
        }

        override fun onProgress(request: BitBoxRequest, progress: Double) {
            // Only the firmware-upgrade path reports progress, and that lives in BitBoxApp now.
        }

        override fun onCommandComplete(request: BitBoxRequest) {
            when (request) {
                BitBoxRequest.INITIALIZE -> {
                    val result = controller.initializeResult()
                    if (result == null) {
                        viewModel.onError(getString(R.string.nc_bitbox_initialize_failed))
                        return
                    }
                    lastFirmwareVersion = result.device.firmwareVersion
                    viewModel.onInitializeResult(
                        isAttestationInvalid = result.isAttestationInvalid,
                        isFirmwareUpgradeRequired = result.device.firmwareUpgradeRequired,
                        isDeviceInitialized = result.device.initialized,
                        // Confluence §0 gates the far end of the window too. BitBoxApp can't fix
                        // it — the device is ahead of Nunchuk, not behind — so this reports
                        // rather than routing to the "continue in BitBoxApp" screen.
                        unsupportedFirmwareMessage = firmwareMessage(tooNew = true)
                            .takeIf { result.device.firmwareVersion.isBitBoxFirmwareTooNew() },
                    )
                }

                BitBoxRequest.MASTER_FINGERPRINT -> {
                    val fingerprint = controller.resultString()
                    // Both accounts of an on-chain key slot have to come off one device, otherwise
                    // the wallet is built from a key the user never meant to add. In verify mode
                    // the same check is what proves the restored seed is the inheritance key.
                    if (expectedXfp.isNotEmpty() && !fingerprint.equals(expectedXfp, ignoreCase = true)) {
                        viewModel.onError(xfpMismatchMessage(fingerprint))
                        return
                    }
                    if (isVerifyXfpOnly) {
                        setResult(
                            RESULT_OK,
                            Intent().apply {
                                putExtra(EXTRA_VERIFIED_XFP, fingerprint.lowercase(Locale.getDefault()))
                            }
                        )
                        finish()
                        return
                    }
                    viewModel.onMasterFingerprintReceived(fingerprint)
                }

                BitBoxRequest.XPUB -> viewModel.onXpubReceived(controller.resultString())

                // Every other command belongs to the sign / health-check flows, not add-key.
                else -> Unit
            }
        }

        override fun onCommandFailed(
            request: BitBoxRequest,
            code: BitBoxErrorCode,
            message: String,
            deviceCode: Int,
        ) {
            viewModel.setScanning(false)
            when {
                // Confluence §6: a lost Noise session is rebuilt rather than reported — the
                // transport is still up, and initialize() replaces the native session. Capped at
                // one attempt per connection so a device failing this way can't spin.
                code.isSessionLost() && !sessionRebuilt -> {
                    sessionRebuilt = true
                    viewModel.setStatus(getString(com.nunchuk.android.core.R.string.nc_bitbox_reconnecting))
                    // initialize() builds a brand-new native session over the transport that is
                    // still connected, and its result restarts the add-key chain from the top —
                    // which is what §6's "restart the operation" amounts to here.
                    controller.initialize()
                }

                // The user said the codes don't match, or declined on the device — not an error
                // worth a toast, just drop back to the picker.
                code.isUserCancellation() -> viewModel.setProcessing(false)

                // Nunchuk can't prepare the device any more, so route these to the hand-off
                // screen rather than showing a raw protocol message.
                code == BitBoxErrorCode.DEVICE_UNINITIALIZED -> viewModel.onDeviceNotReady()

                code == BitBoxErrorCode.ATTESTATION -> viewModel.onAttestationInvalid()

                // §6 wants the firmware's own message: "unsupported" covers both too old and
                // too new, and only the message says which.
                // "Unsupported" covers both ends of the window and they need opposite actions,
                // so name the version and say which way to go.
                code == BitBoxErrorCode.UNSUPPORTED_FIRMWARE -> viewModel.onError(
                    firmwareMessage(tooNew = lastFirmwareVersion.isBitBoxFirmwareTooNew())
                )

                // §6's operation errors: the message alone rarely says which device state was
                // wrong, so the firmware's own error number goes with it.
                code.isOperationError() && deviceCode != 0 -> viewModel.onError(
                    getString(
                        com.nunchuk.android.core.R.string.nc_bitbox_device_error,
                        message,
                        deviceCode,
                    )
                )

                else -> viewModel.onError(message)
            }
        }

        override fun onReboot(request: BitBoxRequest) {
            // Nothing in add-key reboots the device (factory reset / firmware live in BitBoxApp).
            viewModel.setProcessing(false)
        }

        override fun onDisconnected() {
            viewModel.setProcessing(false)
            viewModel.setStatus(getString(R.string.nc_bitbox_disconnected))
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
                viewModel.setStatus(getString(R.string.nc_bitbox_bluetooth_off))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.setMembershipFlow(isMembershipFlow)
        if (isMembershipFlow) {
            // Assisted/group membership wallets take multisig keys only, so the config is fixed here
            // instead of on the (skipped) select-wallet-type step.
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
                val currentEntry by navController.currentBackStackEntryAsState()
                // Screens that report progress themselves: the picker spins its Connect button,
                // and confirm-pairing has the inline spinner beside "Confirm the code on your
                // BitBox". The modal below is for the steps that show nothing of their own.
                val hasInlineProgress = currentEntry?.destination?.let { destination ->
                    destination.hasRoute<BitBoxDeviceScanRoute>() ||
                        destination.hasRoute<BitBoxConfirmPairingRoute>()
                } == true

                LaunchedEffect(Unit) {
                    viewModel.event.collect { event ->
                        when (event) {
                            BitBoxScanEvent.ReadMasterFingerprint -> {
                                // Over BLE the app side is authenticated by bonding, so pairing
                                // can resolve without the user ever answering — take the code
                                // screen down ourselves rather than leaving it stranded.
                                navController.dismissConfirmPairing()
                                controller.getMasterFingerprint()
                            }

                            is BitBoxScanEvent.FetchXpub ->
                                controller.getExtendedPublicKey(event.derivationPath)

                            is BitBoxScanEvent.NavigateToConfirmPairing ->
                                // The controller shows a code once per session, but guard the
                                // destination anyway so a re-emit can't stack a second copy.
                                if (navController.currentDestination
                                        ?.hasRoute<BitBoxConfirmPairingRoute>() != true
                                ) {
                                    navController.navigateToBitBoxConfirmPairing()
                                }

                            BitBoxScanEvent.NavigateToBitBoxAppRequired -> {
                                navController.dismissConfirmPairing()
                                navController.navigateToBitBoxAppRequired()
                            }

                            BitBoxScanEvent.NavigateToAttestationWarning -> {
                                navController.dismissConfirmPairing()
                                navController.navigateToBitBoxAttestationWarning()
                            }

                            BitBoxScanEvent.NavigateToSetKeyName ->
                                if (navController.currentDestination?.route != bitBoxSetKeyNameRoute) {
                                    navController.navigateToBitBoxSetKeyName()
                                }

                            is BitBoxScanEvent.OpenSignerInfo -> {
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
                                        activityContext = this@BitBoxActivity,
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

                            is BitBoxScanEvent.Error -> {
                                navController.dismissConfirmPairing()
                                NCToastMessage(this@BitBoxActivity).showError(event.message)
                            }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = BitBoxIntroRoute,
                ) {
                    bitBoxIntro(
                        onBack = { finish() },
                        // The prepare screen leads for both transports — settled on the thread
                        // (Trezor does this; Ledger shows it for Bluetooth only).
                        onAddViaBluetooth = {
                            viewModel.setTransport(isUsb = false)
                            navController.navigateToBitBoxInstruction(isUsb = false)
                        },
                        onAddViaUsb = {
                            viewModel.setTransport(isUsb = true)
                            navController.navigateToBitBoxInstruction(isUsb = true)
                        },
                        onAddViaDesktop = {
                            setResult(
                                RESULT_OK,
                                Intent().apply {
                                    putExtra(EXTRA_RESULT_ACTION, RESULT_ACTION_OPEN_DESKTOP_FLOW)
                                }
                            )
                            finish()
                        },
                        // Verifying a backup means reading this device; the desktop app can't
                        // stand in for it.
                        isAddViaDesktopEnabled = isMembershipFlow && !isVerifyXfpOnly,
                    )
                    bitBoxInstruction(
                        onBack = { navController.popBackStack() },
                        onContinue = { isUsb ->
                            if (isMembershipFlow) {
                                // Config is fixed for membership wallets, so skip screen 03.
                                navController.navigateToBitBoxDeviceScan()
                                startScanForTransport(isUsb)
                            } else {
                                navController.navigateToBitBoxSelectWalletType(isUsb = isUsb)
                            }
                        },
                        hasSelectWalletTypeStep = !isMembershipFlow,
                    )
                    bitBoxSelectWalletType(
                        onBack = { navController.popBackStack() },
                        onContinue = { isUsb, isSingleSig, addressType, accountIndex ->
                            viewModel.setWalletConfig(
                                walletType = if (isSingleSig) WalletType.SINGLE_SIG else WalletType.MULTI_SIG,
                                addressType = addressType,
                                index = accountIndex,
                            )
                            navController.navigateToBitBoxDeviceScan()
                            startScanForTransport(isUsb)
                        },
                    )
                    bitBoxDeviceScan(
                        onBack = {
                            controller.close()
                            if (!navController.popBackStack()) finish()
                        },
                        isScanning = { state.isScanning },
                        devices = { state.devices },
                        selectedAddress = { state.selectedAddress },
                        statusText = { state.statusText },
                        isProcessing = { state.isProcessing },
                        onRescan = { startScanForTransport(state.isUsbTransport) },
                        onRefreshUsb = { controller.refreshUsb() },
                        onSelectDevice = { device -> viewModel.selectDevice(device.id) },
                        onConnect = {
                            val device = state.selectedDevice ?: return@bitBoxDeviceScan
                            viewModel.setProcessing(true)
                            sessionRebuilt = false
                            controller.connect(device)
                            // Every BitBox session opens with initialize(); the result decides
                            // whether the device is usable at all.
                            controller.whenReady { controller.initialize() }
                        },
                    )
                    bitBoxConfirmPairing(
                        pairingCode = { state.pairingCode },
                        onBack = { navController.popBackStack() },
                        onConfirm = { accepted ->
                            navController.popBackStack()
                            controller.confirmPairing(accepted)
                        },
                    )
                    bitBoxAppRequired(
                        onBack = { navController.popBackStack() },
                        onOpenBitBoxApp = { openBitBoxApp() },
                        onScanAgain = {
                            // Back to the picker so a device prepared in the meantime is picked
                            // up without restarting the flow — over the transport the user chose,
                            // so a USB user isn't asked for the Bluetooth permission.
                            navController.popBackStack()
                            startScanForTransport(state.isUsbTransport)
                        },
                    )
                    bitBoxAttestationWarning(
                        onDisconnect = {
                            controller.close()
                            finish()
                        },
                    )
                    // Standalone add-key only: membership flows auto-name the key and never
                    // reach this step.
                    bitBoxSetKeyName(
                        defaultName = { state.defaultSignerName },
                        onBack = { navController.popBackStack() },
                        onContinue = { name -> viewModel.createBitBoxSigner(name) },
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

                // Only the steps without their own indicator get the modal — in practice the
                // create-signer wait on "Name your key". Stacking it over a screen that already
                // shows progress double-reports the same wait, and over confirm-pairing it also
                // covered the two answer buttons that step depends on.
                if (state.isProcessing && !hasInlineProgress) {
                    NcLoadingDialog(onDismiss = {})
                }
            }
        }
    }

    /**
     * Closes the confirm-pairing screen if it is the one on top. Pairing can finish without the
     * user pressing either button — over BLE, and whenever the device rejects or the session
     * fails — so every exit from initialization calls this rather than relying on the buttons.
     */
    private fun NavHostController.dismissConfirmPairing() {
        if (currentDestination?.hasRoute<BitBoxConfirmPairingRoute>() == true) popBackStack()
    }

    /** USB needs no Bluetooth permission — just list what's attached. */
    private fun startScanForTransport(isUsb: Boolean) {
        if (isUsb) controller.refreshUsb() else ensurePermissionThenScan()
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

    /**
     * Firmware copy naming the version and the direction to move in. Falls back to copy that
     * covers both directions when the version is unknown — better than telling someone to update
     * a device that is already ahead of us.
     */
    private fun firmwareMessage(tooNew: Boolean): String {
        val version = lastFirmwareVersion.trim()
        if (version.isEmpty()) {
            return getString(com.nunchuk.android.core.R.string.nc_bitbox_firmware_unsupported)
        }
        val res = if (tooNew) {
            com.nunchuk.android.core.R.string.nc_bitbox_firmware_too_new
        } else {
            com.nunchuk.android.core.R.string.nc_bitbox_firmware_too_old
        }
        return getString(res, version)
    }

    /** Opens BitBoxApp if it's installed, otherwise its Play listing. */
    private fun openBitBoxApp() {
        val launch = packageManager.getLaunchIntentForPackage(BITBOX_APP_PACKAGE)
        if (launch != null) {
            startActivity(launch)
            return
        }
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$BITBOX_APP_PACKAGE"))
            )
        }.onFailure { error ->
            if (error is ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$BITBOX_APP_PACKAGE"),
                    )
                )
            } else {
                NCToastMessage(this).showError(getString(R.string.nc_bitbox_open_app_failed))
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
        viewModel.setStatus(getString(com.nunchuk.android.core.R.string.nc_bitbox_scanning))
        viewModel.setScanning(true)
        controller.startScan()
    }

    override fun onDestroy() {
        controller.close()
        super.onDestroy()
    }

    companion object {
        private const val BITBOX_APP_PACKAGE = "ch.shiftcrypto.bitboxapp"

        const val EXTRA_IS_MEMBERSHIP_FLOW = "extra_is_membership_flow"
        const val EXTRA_ACCOUNT_INDEX = "extra_account_index"
        const val EXTRA_EXPECTED_XFP = "extra_expected_xfp"
        const val EXTRA_VERIFY_XFP_ONLY = "extra_verify_xfp_only"
        const val EXTRA_RESULT_ACTION = "extra_result_action"

        /** Lowercased fingerprint of the connected device, returned by [verifyXfpOnly]. */
        const val EXTRA_VERIFIED_XFP = GlobalResultKey.EXTRA_VERIFIED_XFP

        /** The user chose to claim the key from the desktop app instead of pairing here. */
        const val RESULT_ACTION_OPEN_DESKTOP_FLOW = "result_action_open_desktop_flow"

        /**
         * "Add key" flow (Get XPUB). [accountIndex] and [expectedXfp] only apply to
         * [isMembershipFlow], which skips the screen where the user would pick the account.
         *
         * [verifyXfpOnly] turns this into a "which device is this?" round trip used by the
         * seed-phrase-backup verification: no key is created and [EXTRA_VERIFIED_XFP] comes back
         * on RESULT_OK. Pass [expectedXfp] with it — a device reporting anything else is turned
         * away here rather than by the caller.
         */
        fun buildIntent(
            activityContext: Context,
            isMembershipFlow: Boolean = false,
            accountIndex: Int = 0,
            expectedXfp: String = "",
            verifyXfpOnly: Boolean = false,
        ): Intent = Intent(activityContext, BitBoxActivity::class.java).apply {
            putExtra(EXTRA_IS_MEMBERSHIP_FLOW, isMembershipFlow)
            putExtra(EXTRA_ACCOUNT_INDEX, accountIndex)
            putExtra(EXTRA_EXPECTED_XFP, expectedXfp)
            putExtra(EXTRA_VERIFY_XFP_ONLY, verifyXfpOnly)
        }
    }
}
