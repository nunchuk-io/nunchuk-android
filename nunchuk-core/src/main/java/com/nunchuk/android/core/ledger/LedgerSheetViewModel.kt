package com.nunchuk.android.core.ledger

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.R
import com.nunchuk.android.core.domain.utils.HealthCheckSingleSignerUseCase
import com.nunchuk.android.core.domain.utils.LedgerTransactionSigner
import com.nunchuk.android.core.domain.utils.LedgerWrongDeviceException
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.HealthStatus
import com.nunchuk.android.type.LedgerUserInteraction
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

/** What the connected Ledger is asked to do once the transport is ready. */
sealed interface LedgerSheetAction {
    /** Label of the sheet's primary button, e.g. "Sign transaction" / "Sign message". */
    @get:StringRes
    val connectButtonText: Int

    /** Confluence "2. Sign transaction": verify the device, register if needed, sign + import. */
    data class SignTransaction(
        val walletId: String,
        val txId: String,
        val masterFingerprint: String,
    ) : LedgerSheetAction {
        override val connectButtonText: Int get() = R.string.nc_ledger_sign_transaction
    }

    /**
     * Same device conversation as [SignTransaction] for a PSBT that isn't a wallet transaction
     * (a dummy transaction): sign [psbt] with the wallet registered as [walletId] and hand the
     * signed PSBT back instead of importing it.
     */
    data class SignPsbt(
        val walletId: String,
        val psbt: String,
        val masterFingerprint: String,
    ) : LedgerSheetAction {
        override val connectButtonText: Int get() = R.string.nc_ledger_sign_transaction
    }

    /**
     * Confluence "Sign message" health check: sign a fixed message with the key at
     * [derivationPath] and verify the signature against the stored [masterFingerprint] signer.
     */
    data class HealthCheck(
        val masterFingerprint: String,
        val derivationPath: String,
    ) : LedgerSheetAction {
        override val connectButtonText: Int get() = R.string.nc_ledger_sign_message
    }
}

data class LedgerSheetUiState(
    val isScanning: Boolean = false,
    val devices: List<LedgerDevice> = emptyList(),
    val selectedDeviceId: String? = null,
    val statusText: String = "",
    /** True once the device conversation has begun; guards re-connecting mid-flow. */
    val isBusy: Boolean = false,
) {
    val selectedDevice: LedgerDevice?
        get() = devices.firstOrNull { it.id == selectedDeviceId }
}

sealed class LedgerSheetEvent {
    /** The signed PSBT was imported into the wallet; the host should refresh and dismiss. */
    data object SignTransactionSuccess : LedgerSheetEvent()

    /** A dummy transaction was signed; the host turns [signedPsbt] into a signature. */
    data class SignPsbtSuccess(val signedPsbt: String) : LedgerSheetEvent()

    /** Health check finished; the outcome is handed to the host (SignerInfo) to display. */
    data class HealthCheckResult(
        val isSuccess: Boolean,
        val errorMessage: String? = null,
    ) : LedgerSheetEvent()

    /** Connected Ledger isn't the signer we're signing for — ask for the right device. */
    data object WrongDevice : LedgerSheetEvent()

    data class Error(val message: String) : LedgerSheetEvent()

    /** Bluetooth is off; the host owns the "enable Bluetooth" launcher. */
    data object RequestEnableBluetooth : LedgerSheetEvent()
}

/**
 * Owns one Ledger sheet session: the BLE/USB transport ([LedgerBleController]), the
 * [LedgerControllerExecutor] adapting it to suspend commands, and whichever
 * [LedgerSheetAction] the host asked for.
 *
 * The transport lives here rather than in the composable so it survives recomposition and is
 * closed exactly once. Everything the transport can't do itself (runtime permissions, turning
 * Bluetooth on) is surfaced as an event for the host to launch.
 */
@HiltViewModel
class LedgerSheetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    nativeSdk: NunchukNativeSdk,
    private val ledgerTransactionSigner: LedgerTransactionSigner,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val healthCheckSingleSignerUseCase: HealthCheckSingleSignerUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LedgerSheetUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<LedgerSheetEvent>()
    val event = _event.asSharedFlow()

    /** Set while a health check is in flight; the signature arrives as a command result. */
    private var pendingHealthCheck: LedgerSheetAction.HealthCheck? = null

    private val listener: LedgerBleController.Listener = object : LedgerBleController.Listener {
        override fun onScanResults(devices: List<LedgerDevice>) = _state.update { state ->
            // Keep the current selection only if it's still present in the refreshed list.
            val selection = state.selectedDeviceId?.takeIf { id -> devices.any { it.id == id } }
            state.copy(devices = devices, selectedDeviceId = selection)
        }

        override fun onScanFinished(found: Int) {
            _state.update { it.copy(isScanning = false) }
            if (found == 0) setStatus(context.getString(R.string.nc_ledger_no_devices_found))
        }

        override fun onConnecting(device: LedgerDevice) {
            _state.update { it.copy(isScanning = false) }
            setStatus(context.getString(R.string.nc_ledger_connecting, device.name))
        }

        override fun onInteraction(interaction: LedgerUserInteraction) {
            setStatus(interactionText(interaction))
        }

        override fun onCommandComplete(request: LedgerRequest, result: String) = when (request) {
            // Health check drives a single command directly; verify the signature it returned.
            LedgerRequest.SIGN_MESSAGE -> verifyHealthCheck(signature = result)
            // Sign transaction runs through the coordinator; hand results to the executor.
            else -> executor.deliverComplete(result)
        }

        override fun onCommandFailed(request: LedgerRequest, statusWord: Int, message: String) {
            if (executor.isAwaiting) {
                executor.deliverFailure(statusWord, message)
            } else {
                // Health check doesn't branch on the status word; surface as a plain error.
                failPendingHealthCheck()
                emit(LedgerSheetEvent.Error(message))
            }
        }

        override fun onError(message: String) {
            // A transport error mid-command fails the in-flight sign; otherwise it's a scan error.
            if (executor.isAwaiting) {
                executor.deliverFailure(statusWord = 0, message = message)
            } else {
                failPendingHealthCheck()
                _state.update { it.copy(isScanning = false) }
                emit(LedgerSheetEvent.Error(message))
            }
        }

        override fun onBluetoothDisabled() {
            _state.update { it.copy(isScanning = false) }
            emit(LedgerSheetEvent.RequestEnableBluetooth)
        }
    }

    private val controller: LedgerBleController = LedgerBleController(context, nativeSdk, listener)
    private val executor: LedgerControllerExecutor = LedgerControllerExecutor(controller)

    fun hasBlePermissions(): Boolean = controller.hasBlePermissions()

    fun requiredPermissions(): Array<String> = controller.requiredPermissions()

    fun onPermissionsDenied() {
        emit(
            LedgerSheetEvent.Error(
                context.getString(R.string.nc_ledger_bluetooth_permission_required)
            )
        )
    }

    /** The user declined the system "turn on Bluetooth" prompt (or it couldn't be shown). */
    fun onBluetoothStillOff() {
        _state.update { it.copy(isScanning = false) }
        setStatus(context.getString(R.string.nc_ledger_bluetooth_off))
    }

    fun startScan() {
        _state.update { it.copy(statusText = "", isScanning = true) }
        controller.startScan()
    }

    fun refreshUsb() = controller.refreshUsb()

    fun selectDevice(deviceId: String) = _state.update { it.copy(selectedDeviceId = deviceId) }

    /**
     * Connects to the selected device and runs [action] once the transport is ready. No-op
     * while a command is already in flight — it's a multi-step device conversation and
     * reconnecting mid-way would drop it.
     */
    fun connect(action: LedgerSheetAction) {
        if (_state.value.isBusy) return
        val device = _state.value.selectedDevice ?: return
        // Busy from the tap, not from the first device command: connecting — and over USB the
        // permission prompt — happens first, and until then nothing on screen would move.
        _state.update { it.copy(isBusy = true) }
        controller.connect(device)
        controller.whenReady { start(action) }
    }

    private fun start(action: LedgerSheetAction) {
        when (action) {
            is LedgerSheetAction.SignTransaction -> signTransaction(action)
            is LedgerSheetAction.SignPsbt -> signPsbt(action)
            is LedgerSheetAction.HealthCheck -> {
                pendingHealthCheck = action
                controller.signMessage(action.derivationPath, HEALTH_CHECK_MESSAGE)
            }
        }
    }

    private fun signTransaction(action: LedgerSheetAction.SignTransaction) = viewModelScope.launch {
        runCatching {
            ledgerTransactionSigner.sign(
                executor = executor,
                walletId = action.walletId,
                txId = action.txId,
                expectedXfp = action.masterFingerprint,
            )
        }.onSuccess {
            _event.emit(LedgerSheetEvent.SignTransactionSuccess)
        }.onFailure { e ->
            emitSignFailure(e)
        }
    }

    private fun signPsbt(action: LedgerSheetAction.SignPsbt) = viewModelScope.launch {
        runCatching {
            ledgerTransactionSigner.signPsbt(
                executor = executor,
                walletId = action.walletId,
                psbt = action.psbt,
                expectedXfp = action.masterFingerprint,
            )
        }.onSuccess { signedPsbt ->
            _event.emit(LedgerSheetEvent.SignPsbtSuccess(signedPsbt))
        }.onFailure { e ->
            emitSignFailure(e)
        }
    }

    /** Reports a failed sign and leaves the sheet retryable (e.g. after connecting the right device). */
    private suspend fun emitSignFailure(e: Throwable) {
        _state.update { it.copy(isBusy = false) }
        when (e) {
            is LedgerWrongDeviceException -> _event.emit(LedgerSheetEvent.WrongDevice)
            else -> _event.emit(LedgerSheetEvent.Error(e.message.orUnknownError()))
        }
    }

    /**
     * Verifies the signature the Ledger just produced over [HEALTH_CHECK_MESSAGE] against the
     * stored remote signer (matched by xfp + path). A signature from the wrong device simply
     * fails verification, so there's no separate device check here.
     */
    private fun verifyHealthCheck(signature: String) {
        val action = pendingHealthCheck ?: return
        pendingHealthCheck = null
        viewModelScope.launch {
            getRemoteSignerUseCase(
                GetRemoteSignerUseCase.Data(
                    id = action.masterFingerprint,
                    derivationPath = action.derivationPath,
                )
            ).onSuccess { signer ->
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
            }.onFailure { e ->
                emitHealthCheckResult(isSuccess = false, errorMessage = e.message)
            }
        }
    }

    /**
     * Clears an in-flight health check so the user can retry with the same sheet. A sign in
     * flight is deliberately left busy — its coroutine is still suspended inside
     * [LedgerTransactionSigner], and reconnecting would start a second one.
     */
    private fun failPendingHealthCheck() {
        if (pendingHealthCheck == null) return
        pendingHealthCheck = null
        _state.update { it.copy(isBusy = false) }
    }

    private suspend fun emitHealthCheckResult(isSuccess: Boolean, errorMessage: String? = null) {
        _state.update { it.copy(isBusy = false) }
        _event.emit(LedgerSheetEvent.HealthCheckResult(isSuccess, errorMessage))
    }

    /**
     * Tears the transport down and clears the picked device / status. The ViewModel is scoped
     * to the host screen, so the sheet calls this when it goes away rather than relying on
     * [onCleared] — otherwise a second attempt would resume a stale session.
     */
    fun closeSession() {
        controller.close()
        pendingHealthCheck = null
        _state.update { LedgerSheetUiState() }
    }

    private fun setStatus(text: String) = _state.update { it.copy(statusText = text) }

    private fun emit(event: LedgerSheetEvent) = viewModelScope.launch {
        // Everything routed through here ends the attempt (denied permission, Bluetooth off,
        // transport error), so release the button for a retry.
        _state.update { it.copy(isBusy = false) }
        _event.emit(event)
    }

    private fun interactionText(interaction: LedgerUserInteraction): String = when (interaction) {
        LedgerUserInteraction.UNLOCK_DEVICE -> context.getString(R.string.nc_ledger_unlock_device)
        LedgerUserInteraction.CONFIRM_OPEN_APP -> context.getString(R.string.nc_ledger_confirm_open_app)
        LedgerUserInteraction.VERIFY_ADDRESS -> context.getString(R.string.nc_ledger_verify_address)
        LedgerUserInteraction.REGISTER_WALLET -> context.getString(R.string.nc_ledger_register_wallet)
        LedgerUserInteraction.SIGN_MESSAGE -> context.getString(R.string.nc_ledger_confirm_sign_message)
        LedgerUserInteraction.SIGN_TRANSACTION -> context.getString(R.string.nc_ledger_confirm_sign_transaction)
        LedgerUserInteraction.NONE -> context.getString(R.string.nc_ledger_communicating)
    }

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }

    companion object {
        // Confluence "Sign message" health-check message. Signed on the device and verified
        // here with HealthCheckSingleSigner (same constant Trezor's health check uses).
        private const val HEALTH_CHECK_MESSAGE = "Run health check"
    }
}
