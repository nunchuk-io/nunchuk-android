package com.nunchuk.android.core.bitbox

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.R
import com.nunchuk.android.core.domain.utils.GetBitBoxSignMessagePathUseCase
import com.nunchuk.android.core.domain.utils.HealthCheckSingleSignerUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.BitBoxErrorCode
import com.nunchuk.android.type.BitBoxUserInteraction
import com.nunchuk.android.type.HealthStatus
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

    data class Error(val message: String) : BitBoxSheetEvent()

    /** Bluetooth is off; the host owns the "enable Bluetooth" launcher. */
    data object RequestEnableBluetooth : BitBoxSheetEvent()
}

/**
 * Owns one BitBox sheet session: the BLE/USB transport ([BitBoxController]) and whichever
 * [BitBoxSheetAction] the host asked for. The BitBox counterpart of [com.nunchuk.android.core.ledger.LedgerSheetViewModel].
 *
 * The transport lives here rather than in the composable so it survives recomposition and is
 * closed exactly once. Everything the transport can't do itself (runtime permissions, turning
 * Bluetooth on) is surfaced as an event for the host to launch.
 *
 * Two things differ from the Ledger sheet, both from the BitBox protocol:
 * - **initialize comes first.** Every session opens with `initialize()`, whose result says
 *   whether the device is genuine, whether its firmware is usable, and whether it has been set
 *   up. The action only starts once that gate passes.
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
) : ViewModel() {

    private val _state = MutableStateFlow(BitBoxSheetUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<BitBoxSheetEvent>()
    val event = _event.asSharedFlow()

    /** The action this attempt is running, kept until it finishes or fails. */
    private var activeAction: BitBoxSheetAction? = null

    /** Signer the in-flight health check verifies against, resolved before the device signs. */
    private var healthCheckSigner: SingleSigner? = null

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
            when (request) {
                BitBoxRequest.INITIALIZE -> onInitialized()
                BitBoxRequest.SIGN_MESSAGE -> onMessageSigned(controller.resultString())
                // Every other command belongs to the sign flows, which this sheet doesn't run yet.
                else -> Unit
            }
        }

        override fun onCommandFailed(
            request: BitBoxRequest,
            code: BitBoxErrorCode,
            message: String,
        ) {
            clearPairingCode()
            when (code) {
                // The user said the codes don't match, or declined on the device — not an error
                // worth reporting, just release the button so they can try again.
                BitBoxErrorCode.PAIRING_REJECTED, BitBoxErrorCode.USER_ABORT -> release()

                // Nunchuk can't prepare the device any more; the add-key flow sends these to a
                // hand-off screen, which a sheet has no room for.
                BitBoxErrorCode.DEVICE_UNINITIALIZED, BitBoxErrorCode.UNSUPPORTED_FIRMWARE ->
                    fail(context.getString(R.string.nc_bitbox_not_ready_error))

                BitBoxErrorCode.ATTESTATION ->
                    fail(context.getString(R.string.nc_bitbox_attestation_error))

                else -> fail(message)
            }
        }

        override fun onReboot(request: BitBoxRequest) {
            // Nothing here reboots the device (factory reset / firmware live in BitBoxApp).
            release()
        }

        override fun onDisconnected() {
            clearPairingCode()
            release()
            setStatus(context.getString(R.string.nc_bitbox_disconnected))
        }

        override fun onError(message: String) {
            clearPairingCode()
            _state.update { it.copy(isScanning = false) }
            fail(message)
        }

        override fun onBluetoothDisabled() {
            _state.update { it.copy(isScanning = false) }
            emit(BitBoxSheetEvent.RequestEnableBluetooth)
        }
    }

    private val controller: BitBoxController = BitBoxController(context, nativeSdk, listener)

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
     * Connects to the selected device and runs [action] once the session is initialized. No-op
     * while a command is already in flight — it's a multi-step device conversation and
     * reconnecting mid-way would drop it.
     */
    fun connect(action: BitBoxSheetAction) {
        if (_state.value.isBusy) return
        val device = _state.value.selectedDevice ?: return
        activeAction = action
        // Busy from the tap, not from the first device command: connecting — and over USB the
        // permission prompt — happens first, and until then nothing on screen would move.
        _state.update { it.copy(isBusy = true) }
        controller.connect(device)
        controller.whenReady { controller.initialize() }
    }

    /** The user's answer to the pairing code shown by [BitBoxSheetUiState.pairingCode]. */
    fun confirmPairing(accepted: Boolean) {
        clearPairingCode()
        controller.confirmPairing(accepted)
    }

    /**
     * The initialize result decides whether this device can be used at all — Confluence §0.
     * Only a genuine, set-up device on usable firmware gets as far as the action.
     */
    private fun onInitialized() {
        val result = controller.initializeResult()
            ?: return fail(context.getString(R.string.nc_bitbox_initialize_failed))
        when {
            result.isAttestationInvalid ->
                fail(context.getString(R.string.nc_bitbox_attestation_error))

            result.device.firmwareUpgradeRequired || !result.device.initialized ->
                fail(context.getString(R.string.nc_bitbox_not_ready_error))

            else -> when (val action = activeAction) {
                is BitBoxSheetAction.HealthCheck -> startHealthCheck(action)
                null -> Unit
            }
        }
    }

    /**
     * Resolves the signer and the path it signs at, then asks the device to sign. The path is
     * the BitBox compact-signature path, not the signer's own — signing at the wrong one would
     * simply fail verification later with nothing to point at.
     */
    private fun startHealthCheck(action: BitBoxSheetAction.HealthCheck) = viewModelScope.launch {
        getRemoteSignerUseCase(
            GetRemoteSignerUseCase.Data(
                id = action.masterFingerprint,
                derivationPath = action.derivationPath,
            )
        ).onSuccess { signer ->
            getBitBoxSignMessagePathUseCase(
                GetBitBoxSignMessagePathUseCase.Param(signer = signer)
            ).onSuccess { path ->
                healthCheckSigner = signer
                controller.signMessage(path, HEALTH_CHECK_MESSAGE)
            }.onFailure { e -> fail(e.message.orUnknownError()) }
        }.onFailure { e -> fail(e.message.orUnknownError()) }
    }

    /**
     * Verifies the signature the BitBox just produced over [HEALTH_CHECK_MESSAGE] against the
     * stored signer. A signature from the wrong device simply fails verification, so there's no
     * separate device check here.
     */
    private fun onMessageSigned(signature: String) {
        val signer = healthCheckSigner ?: return release()
        healthCheckSigner = null
        activeAction = null
        viewModelScope.launch {
            healthCheckSingleSignerUseCase(
                HealthCheckSingleSignerUseCase.Param(
                    signer = signer,
                    message = HEALTH_CHECK_MESSAGE,
                    signature = signature,
                )
            ).onSuccess { status ->
                emitHealthCheckResult(isSuccess = status == HealthStatus.SUCCESS)
            }.onFailure { e ->
                emitHealthCheckResult(isSuccess = false, errorMessage = e.message)
            }
        }
    }

    private suspend fun emitHealthCheckResult(isSuccess: Boolean, errorMessage: String? = null) {
        _state.update { it.copy(isBusy = false) }
        _event.emit(BitBoxSheetEvent.HealthCheckResult(isSuccess, errorMessage))
    }

    /**
     * Tears the transport down and clears the picked device / status. The ViewModel is scoped to
     * the host screen, so the sheet calls this when it goes away rather than relying on
     * [onCleared] — otherwise a second attempt would resume a stale session.
     */
    fun closeSession() {
        controller.close()
        activeAction = null
        healthCheckSigner = null
        _state.update { BitBoxSheetUiState() }
    }

    /** Ends the attempt without reporting anything (the user cancelled). */
    private fun release() {
        activeAction = null
        healthCheckSigner = null
        _state.update { it.copy(isBusy = false) }
    }

    /**
     * Reports a failed attempt and leaves the sheet retryable. Deliberately *not* a
     * [BitBoxSheetEvent.HealthCheckResult]: that result dismisses the sheet and posts a verdict,
     * and a device that never signed hasn't produced one.
     */
    private fun fail(message: String) {
        release()
        emit(BitBoxSheetEvent.Error(message))
    }

    private fun clearPairingCode() = _state.update { it.copy(pairingCode = null) }

    private fun setStatus(text: String) = _state.update { it.copy(statusText = text) }

    private fun emit(event: BitBoxSheetEvent) = viewModelScope.launch {
        // Everything routed through here ends the attempt (denied permission, Bluetooth off,
        // transport error), so release the button for a retry. The health-check verdict goes out
        // through emitHealthCheckResult instead, which dismisses the sheet.
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
