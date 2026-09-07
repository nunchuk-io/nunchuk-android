package com.nunchuk.android.signer.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.utils.GetBip32PathUseCase
import com.nunchuk.android.core.ledger.LedgerDevice
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

data class LedgerScanUiState(
    val isScanning: Boolean = false,
    val devices: List<LedgerDevice> = emptyList(),
    val selectedAddress: String? = null,
    val statusText: String = "",
    val isProcessing: Boolean = false,
    // Wallet config chosen on the "Select wallet & address type" screen; drives get-xpub + createSigner.
    val walletType: WalletType = WalletType.SINGLE_SIG,
    val addressType: AddressType = AddressType.NATIVE_SEGWIT,
    val accountIndex: Int = 0,
    /**
     * Consecutive accounts to read in this one session, starting at [accountIndex]. A miniscript
     * "Reuse keys across policies" slot holds one xpub per policy, and Ledger hands them all over
     * in the same connection (only the unlock / open-Bitcoin-app prompts are per session), so the
     * user pairs the device once. 1 for every other caller.
     */
    val accountCount: Int = 1,
    // Set once the xpub is fetched: the signer waits on the name step (standalone flow) and/or
    // the replace-key confirmation before being persisted.
    val pendingSigner: SingleSigner? = null,
    /** Accounts read after [pendingSigner], created alongside it. Empty unless [accountCount] > 1. */
    val extraSigners: List<SingleSigner> = emptyList(),
    val existingKeyType: ResultExistingKey? = null,
    val replaceExistingKey: Boolean = false,
    // Prefilled into the "Name your key" field; the connected device name when we have one.
    val defaultSignerName: String = "",
) {
    val selectedDevice: LedgerDevice?
        get() = devices.firstOrNull { it.id == selectedAddress }
}

sealed class LedgerScanEvent {
    data object NavigateToSetKeyName : LedgerScanEvent()

    /** Another account is still needed — read the xpub at [index] over the open session. */
    data class FetchXpub(val index: Int) : LedgerScanEvent()

    data class OpenSignerInfo(val signer: SingleSigner) : LedgerScanEvent()

    data class Error(val message: String) : LedgerScanEvent()
}

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val createSignerUseCase: CreateSignerUseCase,
    private val getBip32PathUseCase: GetBip32PathUseCase,
    private val checkExistingKeyUseCase: CheckExistingKeyUseCase,
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val pushEventManager: PushEventManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LedgerScanUiState())
    val state = _state.asStateFlow()

    /** Key names already taken in the app, so a generated name never collides with one. */
    private var existingSignerNames: List<String> = emptyList()

    private val _event = MutableSharedFlow<LedgerScanEvent>()
    val event = _event.asSharedFlow()

    /** Assisted/group membership flows auto-name the key; standalone lets the user name it. */
    private var isMembershipFlow: Boolean = false

    /** Accounts read from the connected device so far, in account-index order. */
    private var collectedSigners: List<SingleSigner> = emptyList()

    fun setMembershipFlow(value: Boolean) {
        isMembershipFlow = value
    }

    fun setScanning(isScanning: Boolean) = _state.update { it.copy(isScanning = isScanning) }

    fun setDevices(devices: List<LedgerDevice>) = _state.update { state ->
        // Keep the current selection only if it's still present in the refreshed list.
        val selection = state.selectedAddress?.takeIf { addr -> devices.any { it.id == addr } }
        state.copy(devices = devices, selectedAddress = selection)
    }

    fun selectDevice(address: String) = _state.update { it.copy(selectedAddress = address) }

    fun setStatus(text: String) = _state.update { it.copy(statusText = text) }

    fun setProcessing(isProcessing: Boolean) = _state.update { it.copy(isProcessing = isProcessing) }

    fun setWalletConfig(walletType: WalletType, addressType: AddressType, index: Int) = _state.update {
        it.copy(walletType = walletType, addressType = addressType, accountIndex = index)
    }

    fun setAccountCount(count: Int) = _state.update { it.copy(accountCount = count.coerceAtLeast(1)) }

    fun onError(message: String) = viewModelScope.launch {
        _state.update { it.copy(isProcessing = false) }
        _event.emit(LedgerScanEvent.Error(message))
    }

    /**
     * Confluence "Get XPUB": builds the SingleSigner from the device master fingerprint
     * + extended public key, tagged as a Ledger hardware signer. Membership flows persist it
     * straight away under the auto-generated name; standalone add-key sends the user to the
     * "Name your key" step first. Either way, a key already in the app has to clear the replace
     * confirmation.
     *
     * When the caller asked for more than one account ([LedgerScanUiState.accountCount]) this is
     * one lap of a loop: the signer is collected and the next account is requested over the same
     * session, so all of them are created together once the last xpub is in.
     */
    fun onXpubReceived(
        masterFingerprint: String,
        xpub: String,
    ) = viewModelScope.launch {
        val config = _state.value
        val index = config.accountIndex + collectedSigners.size
        _state.update { it.copy(isProcessing = true) }
        getBip32PathUseCase(
            GetBip32PathUseCase.Param(
                index = index,
                walletType = config.walletType,
                addressType = config.addressType,
            )
        ).onSuccess { path ->
            val signer = SingleSigner(
                name = SignerTag.LEDGER.formattedName,
                xpub = xpub,
                derivationPath = path,
                masterFingerprint = masterFingerprint.lowercase(),
                type = SignerType.HARDWARE,
                tags = listOf(SignerTag.LEDGER),
            )
            collectedSigners = collectedSigners + signer
            if (collectedSigners.size < config.accountCount) {
                _event.emit(LedgerScanEvent.FetchXpub(config.accountIndex + collectedSigners.size))
                return@launch
            }
            // The replace confirmation is about the device, not one of its accounts, so it is
            // asked once — for the first account — and applies to every key created below.
            val firstSigner = collectedSigners.first()
            checkExistingKeyUseCase(CheckExistingKeyUseCase.Params(singleSigner = firstSigner))
                .onSuccess { existingKeyType ->
                    // Read the names before either naming path needs them, so a slow signer
                    // load can't hand out a name that is already taken.
                    refreshExistingSignerNames()
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            pendingSigner = firstSigner,
                            extraSigners = collectedSigners.drop(1),
                            existingKeyType = existingKeyType.takeIf { type -> type != ResultExistingKey.None },
                            replaceExistingKey = false,
                            // Spec names the key after the connected bluetooth/usb device.
                            defaultSignerName = uniqueName(
                                config.selectedDevice?.name?.takeIf(String::isNotBlank)
                                    ?: SignerTag.LEDGER.formattedName
                            ),
                        )
                    }
                    if (existingKeyType == ResultExistingKey.None) {
                        continueToNaming()
                    }
                }.onFailure { e -> onError(e.message.orUnknownError()) }
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
     * Membership flows auto-name the key "Ledger", stepping up to "Ledger 2", "Ledger 3"… when
     * that name is taken. Standalone add-key stops on the "Name your key" step instead and lets
     * the user name it.
     */
    private suspend fun continueToNaming() {
        if (isMembershipFlow) {
            createSigner(uniqueName(SignerTag.LEDGER.formattedName))
        } else {
            _event.emit(LedgerScanEvent.NavigateToSetKeyName)
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

    fun createLedgerSigner(name: String) {
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
                _event.emit(LedgerScanEvent.Error(e.message.orUnknownError()))
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
        firstCreated?.let { _event.emit(LedgerScanEvent.OpenSignerInfo(it)) }
    }
}
