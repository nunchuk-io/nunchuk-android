package com.nunchuk.android.signer.bitbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.bitbox.BitBoxDevice
import com.nunchuk.android.core.domain.utils.GetBip32PathUseCase
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.formattedName
import com.nunchuk.android.core.util.generateUniqueSignerName
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CheckExistingKeyUseCase
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.ResultExistingKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the BitBox add-key flow, mirroring [com.nunchuk.android.signer.ledger.LedgerViewModel]
 * with the BitBox-only steps added: the pairing-code confirmation, and the two dead ends
 * (BitBoxApp required, attestation invalid).
 *
 * The host activity owns the transport and only reports device outcomes here; every decision
 * about where the flow goes next is made in this class and delivered as a [BitBoxScanEvent], so
 * readiness is never evaluated in two places.
 */
@HiltViewModel
class BitBoxViewModel @Inject constructor(
    private val createSignerUseCase: CreateSignerUseCase,
    private val getBip32PathUseCase: GetBip32PathUseCase,
    private val checkExistingKeyUseCase: CheckExistingKeyUseCase,
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val pushEventManager: PushEventManager,
) : ViewModel() {

    private val _state = MutableStateFlow(BitBoxScanUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<BitBoxScanEvent>()
    val event = _event.asSharedFlow()

    /** Key names already taken in the app, so a generated name never collides with one. */
    private var existingSignerNames: List<String> = emptyList()

    /** Assisted/group membership flows auto-name the key; standalone lets the user name it. */
    private var isMembershipFlow: Boolean = false

    /** Accounts read from the connected device so far, in account-index order. */
    private var collectedSigners: List<SingleSigner> = emptyList()

    fun setMembershipFlow(value: Boolean) {
        isMembershipFlow = value
    }

    fun setScanning(isScanning: Boolean) = _state.update { it.copy(isScanning = isScanning) }

    fun setDevices(devices: List<BitBoxDevice>) = _state.update { state ->
        // Keep the current selection only if it's still present in the refreshed list.
        val selection = state.selectedAddress?.takeIf { addr -> devices.any { it.id == addr } }
        state.copy(devices = devices, selectedAddress = selection)
    }

    fun selectDevice(address: String) = _state.update { it.copy(selectedAddress = address) }

    fun setStatus(text: String) = _state.update { it.copy(statusText = text) }

    fun setProcessing(isProcessing: Boolean) = _state.update { it.copy(isProcessing = isProcessing) }

    fun setTransport(isUsb: Boolean) = _state.update { it.copy(isUsbTransport = isUsb) }

    fun setWalletConfig(walletType: WalletType, addressType: AddressType, index: Int) =
        _state.update {
            it.copy(walletType = walletType, addressType = addressType, accountIndex = index)
        }

    fun setAccountCount(count: Int) = _state.update { it.copy(accountCount = count.coerceAtLeast(1)) }

    fun onError(message: String) = viewModelScope.launch {
        _state.update { it.copy(isProcessing = false) }
        _event.emit(BitBoxScanEvent.Error(message))
    }

    /** An AWAITING_USER step surfaced a pairing code — send the user to compare it. */
    fun onPairingCode(code: String) = viewModelScope.launch {
        _state.update { it.copy(pairingCode = code) }
        _event.emit(BitBoxScanEvent.NavigateToConfirmPairing(code))
    }

    /**
     * Initialization finished. Gates are checked in the order the Confluence page prescribes:
     * attestation first (a device we can't authenticate must not be used at all), then whether
     * the firmware is upgrade-only, then whether the device has been set up. Anything short of
     * ready hands off to BitBoxApp; a ready device continues to the fingerprint.
     */
    /**
     * @param unsupportedFirmwareMessage set when the device is *ahead* of the integration
     * (Confluence §0's "10.0 or newer"). That is the one not-usable case BitBoxApp can't fix, so
     * it is reported rather than routed to the "continue in BitBoxApp" hand-off.
     */
    fun onInitializeResult(
        isAttestationInvalid: Boolean,
        isFirmwareUpgradeRequired: Boolean,
        isDeviceInitialized: Boolean,
        unsupportedFirmwareMessage: String? = null,
    ) = viewModelScope.launch {
        when {
            isAttestationInvalid -> emitNotUsable(BitBoxScanEvent.NavigateToAttestationWarning)
            unsupportedFirmwareMessage != null -> onError(unsupportedFirmwareMessage)
            isFirmwareUpgradeRequired || !isDeviceInitialized ->
                emitNotUsable(BitBoxScanEvent.NavigateToBitBoxAppRequired)
            // Stay in the processing state: the device conversation continues straight away.
            else -> _event.emit(BitBoxScanEvent.ReadMasterFingerprint)
        }
    }

    /** The device reported it is uninitialized or on upgrade-only firmware — design screen 05B. */
    fun onDeviceNotReady() = viewModelScope.launch {
        emitNotUsable(BitBoxScanEvent.NavigateToBitBoxAppRequired)
    }

    /** The device failed its genuineness check — design screen 05C. */
    fun onAttestationInvalid() = viewModelScope.launch {
        emitNotUsable(BitBoxScanEvent.NavigateToAttestationWarning)
    }

    /** Both dead ends stop the spinner: nothing further is asked of the device. */
    private suspend fun emitNotUsable(event: BitBoxScanEvent) {
        _state.update { it.copy(isProcessing = false) }
        _event.emit(event)
    }

    /**
     * The device reported its master fingerprint. Resolve the derivation path for the chosen
     * wallet config and ask the caller to read the xpub at it.
     */
    fun onMasterFingerprintReceived(masterFingerprint: String) = viewModelScope.launch {
        val config = _state.value
        _state.update { it.copy(isProcessing = true, masterFingerprint = masterFingerprint) }
        getBip32PathUseCase(
            GetBip32PathUseCase.Param(
                index = config.accountIndex,
                walletType = config.walletType,
                addressType = config.addressType,
            )
        ).onSuccess { path ->
            _state.update { it.copy(derivationPath = path) }
            _event.emit(BitBoxScanEvent.FetchXpub(path))
        }.onFailure { e -> onError(e.message.orUnknownError()) }
    }

    /**
     * Confluence "Get XPUB": builds the SingleSigner from the master fingerprint read earlier
     * plus this extended public key, tagged as a BitBox hardware signer. Membership flows persist
     * it straight away under the auto-generated name; standalone add-key sends the user to the
     * "Name your key" step first. Either way, a key already in the app has to clear the replace
     * confirmation.
     */
    fun onXpubReceived(xpub: String) = viewModelScope.launch {
        val config = _state.value
        if (config.masterFingerprint.isEmpty() || config.derivationPath.isEmpty()) {
            onError("Missing BitBox key details")
            return@launch
        }
        _state.update { it.copy(isProcessing = true) }
        val signer = SingleSigner(
            name = SignerTag.BITBOX.formattedName,
            xpub = xpub,
            derivationPath = config.derivationPath,
            masterFingerprint = config.masterFingerprint.lowercase(),
            type = SignerType.HARDWARE,
            tags = listOf(SignerTag.BITBOX),
        )
        collectedSigners = collectedSigners + signer
        if (collectedSigners.size < config.accountCount) {
            requestNextAccount(config.accountIndex + collectedSigners.size)
            return@launch
        }
        // The replace confirmation is about the device, not one of its accounts, so it is asked
        // once — for the first account — and applies to every key created below.
        val firstSigner = collectedSigners.first()
        checkExistingKeyUseCase(CheckExistingKeyUseCase.Params(singleSigner = firstSigner))
            .onSuccess { existingKeyType ->
                // Read the names before either naming path needs them, so a slow signer load
                // can't hand out a name that is already taken.
                refreshExistingSignerNames()
                _state.update {
                    it.copy(
                        isProcessing = false,
                        pendingSigner = firstSigner,
                        extraSigners = collectedSigners.drop(1),
                        existingKeyType = existingKeyType.takeIf { type -> type != ResultExistingKey.None },
                        replaceExistingKey = false,
                        // Design names the key after the connected device.
                        defaultSignerName = uniqueName(
                            config.selectedDevice?.name?.takeIf(String::isNotBlank)
                                ?: SignerTag.BITBOX.formattedName
                        ),
                    )
                }
                if (existingKeyType == ResultExistingKey.None) {
                    continueToNaming()
                }
            }.onFailure { e -> onError(e.message.orUnknownError()) }
    }

    /**
     * Reads the next account over the session that is already open. Only the pairing/unlock steps
     * are per-session, so this needs nothing further from the user.
     */
    private suspend fun requestNextAccount(index: Int) {
        val config = _state.value
        getBip32PathUseCase(
            GetBip32PathUseCase.Param(
                index = index,
                walletType = config.walletType,
                addressType = config.addressType,
            )
        ).onSuccess { path ->
            _state.update { it.copy(derivationPath = path) }
            _event.emit(BitBoxScanEvent.FetchXpub(path))
        }.onFailure { e -> onError(e.message.orUnknownError()) }
    }

    fun confirmExistingKeyDialog() {
        if (_state.value.pendingSigner == null) return
        _state.update { it.copy(existingKeyType = null, replaceExistingKey = true) }
        viewModelScope.launch { continueToNaming() }
    }

    fun dismissExistingKeyDialog() {
        collectedSigners = emptyList()
        _state.update {
            it.copy(
                pendingSigner = null,
                extraSigners = emptyList(),
                existingKeyType = null,
                replaceExistingKey = false,
            )
        }
    }

    /**
     * Membership flows auto-name the key "BitBox", stepping up to "BitBox 2", "BitBox 3"… when
     * that name is taken. Standalone add-key stops on the "Name your key" step instead.
     */
    private suspend fun continueToNaming() {
        if (isMembershipFlow) {
            createSigner(uniqueName(SignerTag.BITBOX.formattedName))
        } else {
            _event.emit(BitBoxScanEvent.NavigateToSetKeyName)
        }
    }

    private fun uniqueName(baseName: String) =
        generateUniqueSignerName(baseName, existingSignerNames)

    private suspend fun refreshExistingSignerNames() {
        runCatching { getCompoundSignersUseCase.execute().first() }
            .onSuccess { (masterSigners, remoteSigners) ->
                existingSignerNames = masterSigners.map { it.name } + remoteSigners.map { it.name }
            }
    }

    fun createBitBoxSigner(name: String) {
        val signerName = name.trim()
        if (signerName.isEmpty()) return
        viewModelScope.launch { createSigner(signerName) }
    }

    /**
     * Persists every account read from the device. The user names the first one; the accounts
     * behind it step up to "<name> 2", "<name> 3"… the same way membership auto-naming does. Each
     * is announced with its own [PushEvent.LocalUserSignerAdded] so a wallet flow waiting on the
     * key picks up all of them (a miniscript reuse slot fills one policy per account).
     */
    private suspend fun createSigner(name: String) {
        val state = _state.value
        val signers = listOfNotNull(state.pendingSigner) + state.extraSigners
        if (signers.isEmpty()) return
        val replace = state.replaceExistingKey
        _state.update { it.copy(isProcessing = true) }
        var firstCreated: SingleSigner? = null
        signers.forEachIndexed { position, signer ->
            val signerName = if (position == 0) name else uniqueName(name)
            val result = createSignerUseCase(
                CreateSignerUseCase.Params(
                    name = signerName,
                    xpub = signer.xpub,
                    type = signer.type,
                    derivationPath = signer.derivationPath,
                    masterFingerprint = signer.masterFingerprint,
                    tags = signer.tags,
                    replace = replace,
                )
            )
            val createdSigner = result.getOrElse { e ->
                _state.update { it.copy(isProcessing = false) }
                _event.emit(BitBoxScanEvent.Error(e.message.orUnknownError()))
                return
            }
            // Keep the generated names apart from each other, not just from the keys that were
            // already in the app when the device was read.
            existingSignerNames = existingSignerNames + createdSigner.name
            pushEventManager.push(PushEvent.LocalUserSignerAdded(createdSigner))
            if (firstCreated == null) firstCreated = createdSigner
        }
        collectedSigners = emptyList()
        _state.update {
            it.copy(
                isProcessing = false,
                pendingSigner = null,
                extraSigners = emptyList(),
                existingKeyType = null,
                replaceExistingKey = false,
            )
        }
        firstCreated?.let { _event.emit(BitBoxScanEvent.OpenSignerInfo(it)) }
    }
}
