package com.nunchuk.android.core.bitbox

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.R
import com.nunchuk.android.core.domain.utils.BitBoxTransactionSigner
import com.nunchuk.android.core.domain.utils.GetBitBoxSignMessagePathUseCase
import com.nunchuk.android.core.domain.utils.HealthCheckSingleSignerUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.BitBoxErrorCode
import com.nunchuk.android.type.BitBoxUserInteraction
import com.nunchuk.android.type.HealthStatus
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the connected BitBox is asked to do once the session is initialized. */
sealed interface BitBoxSheetAction {
    /** Label of the sheet's primary button. */
    @get:StringRes
    val connectButtonText: Int

    /**
     * Confluence "4. Sign message" used as a health check: sign the health-check message at the
     * signer's BitBox signing path and verify the signature against the stored signer, matched
     * by [masterFingerprint] + [derivationPath].
     */
    data class HealthCheck(
        val masterFingerprint: String,
        val derivationPath: String,
    ) : BitBoxSheetAction {
        override val connectButtonText: Int get() = R.string.nc_ledger_sign_message
    }

    /**
     * Confluence "3. Sign transaction" for a PSBT that isn't a wallet transaction — a dummy
     * transaction: register [walletId]'s policy if needed, sign [psbt], and hand the signed PSBT
     * back for the caller to extract a signature from rather than importing it.
     */
    data class SignPsbt(
        val walletId: String,
        val psbt: String,
        val masterFingerprint: String,
    ) : BitBoxSheetAction {
        override val connectButtonText: Int get() = R.string.nc_ledger_sign_transaction
    }

    /**
     * [SignPsbt] for a [wallet] that isn't stored locally, so it can't be looked up by id: the
     * sign-in dummy transaction, whose wallet is parsed from the BSMS the user pasted.
     */
    data class SignPsbtWithWallet(
        val wallet: Wallet,
        val psbt: String,
        val masterFingerprint: String,
    ) : BitBoxSheetAction {
        override val connectButtonText: Int get() = R.string.nc_ledger_sign_transaction
    }
}

data class BitBoxSheetUiState(
    val isScanning: Boolean = false,
    val devices: List<BitBoxDevice> = emptyList(),
    val selectedDeviceId: String? = null,
    val statusText: String = "",
    /** True once the device conversation has begun; guards re-connecting mid-flow. */
    val isBusy: Boolean = false,
    /**
     * Pairing code to compare with the device, non-null only while that answer is outstanding.
     * The sheet shows the pairing body in place of the picker for as long as it is set.
     */
    val pairingCode: String? = null,
) {
    val selectedDevice: BitBoxDevice?
        get() = devices.firstOrNull { it.id == selectedDeviceId }
}

sealed class BitBoxSheetEvent {
    /** Health check finished; the outcome is handed to the host (SignerInfo) to display. */
    data class HealthCheckResult(
        val isSuccess: Boolean,
        val errorMessage: String? = null,
    ) : BitBoxSheetEvent()

    /** A dummy transaction was signed; the host turns [signedPsbt] into a signature. */
    data class SignPsbtSuccess(val signedPsbt: String) : BitBoxSheetEvent()

    /** Connected BitBox isn't the signer we're signing for — ask for the right device. */
    data object WrongDevice : BitBoxSheetEvent()

    data class Error(val message: String) : BitBoxSheetEvent()

    /** Bluetooth is off; the host owns the "enable Bluetooth" launcher. */
    data object RequestEnableBluetooth : BitBoxSheetEvent()
}

/**
 * Owns one BitBox sheet session: the BLE/USB transport ([BitBoxController]), the
 * [BitBoxCommandExecutor] adapting it to suspend commands, and whichever [BitBoxSheetAction] the
 * host asked for. The BitBox counterpart of
 * [com.nunchuk.android.core.ledger.LedgerSheetViewModel].
 *
 * The transport lives here rather than in the composable so it survives recomposition and is
 * closed exactly once. Everything the transport can't do itself (runtime permissions, turning
 * Bluetooth on) is surfaced as an event for the host to launch.
 *
 * Two things differ from the Ledger sheet, both from the BitBox protocol:
 * - **initialize comes first.** Every session opens with `initialize()`, whose result says
 *   whether the device is genuine, whether its firmware is usable, and whether it has been set
 *   up. The action only starts once that gate passes, so it is the head of every sequence
 *   rather than something the host does beforehand.
 * - **pairing may need answering.** A first-time pairing surfaces a code to compare with the
 *   device, which the sheet shows in place of the picker. Pairing data is stored per Nunchuk
 *   account/chain, so a known device never shows one.
 */
@HiltViewModel
class BitBoxSheetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    nativeSdk: NunchukNativeSdk,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val getBitBoxSignMessagePathUseCase: GetBitBoxSignMessagePathUseCase,
    private val healthCheckSingleSignerUseCase: HealthCheckSingleSignerUseCase,
    private val transactionSigner: BitBoxTransactionSigner,
) : ViewModel() {

    private val _state = MutableStateFlow(BitBoxSheetUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<BitBoxSheetEvent>()
    val event = _event.asSharedFlow()

    /** The running device conversation, cancelled when the sheet goes away mid-flow. */
    private var actionJob: Job? = null

    private val listener = object : BitBoxController.Listener {
        override fun onScanResults(devices: List<BitBoxDevice>) = _state.update { state ->
            // Keep the current selection only if it's still present in the refreshed list.
            val selection = state.selectedDeviceId?.takeIf { id -> devices.any { it.id == id } }
            state.copy(devices = devices, selectedDeviceId = selection)
        }

        override fun onScanFinished(found: Int) {
            _state.update { it.copy(isScanning = false) }
            if (found == 0) setStatus(context.getString(R.string.nc_bitbox_no_devices_found))
        }

        override fun onConnecting(device: BitBoxDevice) {
            _state.update { it.copy(isScanning = false) }
            setStatus(context.getString(R.string.nc_ledger_connecting, device.name))
        }

        override fun onInteraction(interaction: BitBoxUserInteraction) {
            setStatus(interaction.statusText(context))
        }

        override fun onPairingCode(code: String) = _state.update { it.copy(pairingCode = code) }

        override fun onProgress(request: BitBoxRequest, progress: Double) {
            // Only the firmware-upgrade path reports progress, and that lives in BitBoxApp now.
        }

        override fun onCommandComplete(request: BitBoxRequest) {
            // Pairing can resolve without either button being pressed (over BLE, bonding
            // authenticates the app side), so take the code down from here rather than from the
            // buttons — and from every command, since the code rides whichever request is
            // outstanding.
            clearPairingCode()
            executor.deliverComplete(request)
        }

        override fun onCommandFailed(
            request: BitBoxRequest,
            code: BitBoxErrorCode,
            message: String,
        ) {
            clearPairingCode()
            executor.deliverFailure(request, code, message)
        }

        override fun onReboot(request: BitBoxRequest) {
            // Nothing here reboots the device (factory reset / firmware live in BitBoxApp), so if
            // one arrives the session is gone and whatever was in flight cannot finish.
            failInFlight(BitBoxErrorCode.SESSION_LOST, disconnectedMessage())
        }

        override fun onDisconnected() {
            clearPairingCode()
            // Nothing resumes across a transport drop, so fail the sequence rather than leaving
            // it suspended on a session that no longer exists.
            failInFlight(BitBoxErrorCode.SESSION_LOST, disconnectedMessage())
            setStatus(disconnectedMessage())
        }

        override fun onError(message: String) {
            clearPairingCode()
            _state.update { it.copy(isScanning = false) }
            // A transport error mid-command fails the in-flight sequence, which reports it;
            // otherwise there is nothing running and it is a scan-time error.
            if (executor.isAwaiting) {
                failInFlight(BitBoxErrorCode.NONE, message)
            } else {
                emit(BitBoxSheetEvent.Error(message))
            }
        }

        override fun onBluetoothDisabled() {
            _state.update { it.copy(isScanning = false) }
            emit(BitBoxSheetEvent.RequestEnableBluetooth)
        }
    }

    private val controller: BitBoxController = BitBoxController(context, nativeSdk, listener)
    private val executor: BitBoxCommandExecutor = BitBoxCommandExecutor(controller)

    fun hasBlePermissions(): Boolean = controller.hasBlePermissions()

    fun requiredPermissions(): Array<String> = controller.requiredPermissions()

    fun onPermissionsDenied() {
        emit(
            BitBoxSheetEvent.Error(
                context.getString(R.string.nc_bitbox_bluetooth_permission_required)
            )
        )
    }

    /** The user declined the system "turn on Bluetooth" prompt (or it couldn't be shown). */
    fun onBluetoothStillOff() {
        _state.update { it.copy(isScanning = false) }
        setStatus(context.getString(R.string.nc_bitbox_bluetooth_off))
    }

    fun startScan() {
        _state.update {
            it.copy(statusText = context.getString(R.string.nc_bitbox_scanning), isScanning = true)
        }
        controller.startScan()
    }

    fun refreshUsb() = controller.refreshUsb()

    fun selectDevice(deviceId: String) = _state.update { it.copy(selectedDeviceId = deviceId) }

    /**
     * Connects to the selected device and runs [action] once the transport is ready. No-op while
     * a command is already in flight — it's a multi-step device conversation and reconnecting
     * mid-way would drop it.
     */
    fun connect(action: BitBoxSheetAction) {
        if (_state.value.isBusy) return
        val device = _state.value.selectedDevice ?: return
        // Busy from the tap, not from the first device command: connecting — and over USB the
        // permission prompt — happens first, and until then nothing on screen would move.
        _state.update { it.copy(isBusy = true) }
        controller.connect(device)
        controller.whenReady { runAction(action) }
    }

    /** The user's answer to the pairing code shown by [BitBoxSheetUiState.pairingCode]. */
    fun confirmPairing(accepted: Boolean) {
        clearPairingCode()
        controller.confirmPairing(accepted)
    }

    /**
     * The whole device conversation for [action], as the sequence Confluence describes. Every
     * BitBox session opens with `initialize()`, and its result is the readiness gate — a device
     * that isn't genuine, isn't set up, or is on unusable firmware is never asked for anything.
     */
    private fun runAction(action: BitBoxSheetAction) {
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            runCatching {
                val result = executor.initialize()
                    ?: throw IllegalStateException(
                        context.getString(R.string.nc_bitbox_initialize_failed)
                    )
                when {
                    result.isAttestationInvalid ->
                        throw IllegalStateException(
                            context.getString(R.string.nc_bitbox_attestation_error)
                        )

                    result.device.firmwareUpgradeRequired || !result.device.initialized ->
                        throw IllegalStateException(
                            context.getString(R.string.nc_bitbox_not_ready_error)
                        )
                }
                when (action) {
                    is BitBoxSheetAction.HealthCheck -> runHealthCheck(action)
                    is BitBoxSheetAction.SignPsbt -> BitBoxSheetEvent.SignPsbtSuccess(
                        transactionSigner.signPsbt(
                            executor = executor,
                            walletId = action.walletId,
                            psbt = action.psbt,
                            expectedXfp = action.masterFingerprint,
                        )
                    )

                    is BitBoxSheetAction.SignPsbtWithWallet -> BitBoxSheetEvent.SignPsbtSuccess(
                        transactionSigner.signPsbt(
                            executor = executor,
                            wallet = action.wallet,
                            psbt = action.psbt,
                            expectedXfp = action.masterFingerprint,
                        )
                    )
                }
            }.onSuccess { outcome ->
                _state.update { it.copy(isBusy = false) }
                _event.emit(outcome)
            }.onFailure { e ->
                reportFailure(e)
            }
        }
    }

    /**
     * Signs the health-check message and verifies it against the stored signer. The path is the
     * BitBox compact-signature path, not the signer's own — signing at the wrong one would fail
     * verification later with nothing to point at.
     *
     * A signature from the wrong device also simply fails verification, so unlike signing there
     * is no separate device check here.
     */
    private suspend fun runHealthCheck(
        action: BitBoxSheetAction.HealthCheck,
    ): BitBoxSheetEvent.HealthCheckResult {
        val signer = getRemoteSignerUseCase(
            GetRemoteSignerUseCase.Data(
                id = action.masterFingerprint,
                derivationPath = action.derivationPath,
            )
        ).getOrThrow()
        val path = getBitBoxSignMessagePathUseCase(
            GetBitBoxSignMessagePathUseCase.Param(signer = signer)
        ).getOrThrow()

        val signature = executor.signMessage(path, HEALTH_CHECK_MESSAGE)

        return healthCheckSingleSignerUseCase(
            HealthCheckSingleSignerUseCase.Param(
                signer = signer,
                message = HEALTH_CHECK_MESSAGE,
                signature = signature,
            )
        ).fold(
            onSuccess = { status ->
                BitBoxSheetEvent.HealthCheckResult(isSuccess = status == HealthStatus.SUCCESS)
            },
            onFailure = { e ->
                BitBoxSheetEvent.HealthCheckResult(isSuccess = false, errorMessage = e.message)
            },
        )
    }

    /**
     * Ends a failed attempt and leaves the sheet retryable. Deliberately never a success-shaped
     * event: a health check whose device never signed hasn't produced a verdict, and reporting
     * one would record "not set up" as a *failed* key.
     */
    private suspend fun reportFailure(e: Throwable) {
        // runCatching catches CancellationException too, and a cancelled sequence is the sheet
        // being dismissed mid-conversation — not something to report.
        if (e is CancellationException) throw e
        _state.update { it.copy(isBusy = false) }
        when {
            // The user said the codes don't match, or declined on the device — they know what
            // happened, so just release the button.
            e is BitBoxCommandException && e.code.isUserCancellation() -> Unit

            e is BitBoxWrongDeviceException -> _event.emit(BitBoxSheetEvent.WrongDevice)

            // Nunchuk can't prepare the device any more; the add-key flow sends these to a
            // hand-off screen, which a sheet has no room for.
            e is BitBoxCommandException && e.code.isDeviceNotReady() -> _event.emit(
                BitBoxSheetEvent.Error(context.getString(R.string.nc_bitbox_not_ready_error))
            )

            e is BitBoxCommandException && e.code == BitBoxErrorCode.ATTESTATION -> _event.emit(
                BitBoxSheetEvent.Error(context.getString(R.string.nc_bitbox_attestation_error))
            )

            else -> _event.emit(BitBoxSheetEvent.Error(e.message.orUnknownError()))
        }
    }

    /**
     * Fails the suspended command, if any, so the sequence unwinds and reports instead of hanging
     * on a session that no longer exists. Releases the button either way — there may have been
     * nothing suspended (a drop between commands), and the sequence setting it again is harmless.
     */
    private fun failInFlight(code: BitBoxErrorCode, message: String) {
        executor.deliverFailure(request = null, code = code, message = message)
        _state.update { it.copy(isBusy = false) }
    }

    /**
     * Tears the transport down and clears the picked device / status. The ViewModel is scoped to
     * the host screen, so the sheet calls this when it goes away rather than relying on
     * [onCleared] — otherwise a second attempt would resume a stale session.
     */
    fun closeSession() {
        // close() doesn't report a disconnect, so cancel the sequence here or it stays suspended
        // on a session that is already gone.
        actionJob?.cancel()
        actionJob = null
        controller.close()
        _state.update { BitBoxSheetUiState() }
    }

    private fun disconnectedMessage() = context.getString(R.string.nc_bitbox_disconnected)

    private fun clearPairingCode() = _state.update { it.copy(pairingCode = null) }

    private fun setStatus(text: String) = _state.update { it.copy(statusText = text) }

    private fun emit(event: BitBoxSheetEvent) = viewModelScope.launch {
        // Everything routed through here ends the attempt (denied permission, Bluetooth off,
        // scan-time error), so release the button for a retry.
        _state.update { it.copy(isBusy = false) }
        _event.emit(event)
    }

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }

    companion object {
        // Confluence "4. Sign message" health check: signed on the device and verified here with
        // HealthCheckSingleSigner (same constant Trezor's and Ledger's health checks use).
        private const val HEALTH_CHECK_MESSAGE = "Run health check"
    }
}

/** The user backed out — on the device or on the pairing screen. Not worth reporting as an error. */
private fun BitBoxErrorCode.isUserCancellation(): Boolean =
    this == BitBoxErrorCode.PAIRING_REJECTED || this == BitBoxErrorCode.USER_ABORT

/** Nunchuk cannot prepare the device any further; it has to be finished in BitBoxApp. */
private fun BitBoxErrorCode.isDeviceNotReady(): Boolean =
    this == BitBoxErrorCode.DEVICE_UNINITIALIZED || this == BitBoxErrorCode.UNSUPPORTED_FIRMWARE
