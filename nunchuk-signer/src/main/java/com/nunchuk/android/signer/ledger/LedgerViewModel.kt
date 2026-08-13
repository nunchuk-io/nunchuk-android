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
    // Set once the xpub is fetched: the signer waits on the name step (standalone flow) and/or
    // the replace-key confirmation before being persisted.
    val pendingSigner: SingleSigner? = null,
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
     */
    fun onXpubReceived(
        masterFingerprint: String,
        xpub: String,
    ) = viewModelScope.launch {
        val config = _state.value
        _state.update { it.copy(isProcessing = true) }
        getBip32PathUseCase(
            GetBip32PathUseCase.Param(
                index = config.accountIndex,
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
            checkExistingKeyUseCase(CheckExistingKeyUseCase.Params(singleSigner = signer))
                .onSuccess { existingKeyType ->
                    // Read the names before either naming path needs them, so a slow signer
                    // load can't hand out a name that is already taken.
                    refreshExistingSignerNames()
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            pendingSigner = signer,
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

    fun dismissExistingKeyDialog() = _state.update {
        it.copy(pendingSigner = null, existingKeyType = null, replaceExistingKey = false)
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

    private suspend fun createSigner(name: String) {
        val signer = _state.value.pendingSigner ?: return
        val replace = _state.value.replaceExistingKey
        _state.update { it.copy(isProcessing = true) }
        createSignerUseCase(
            CreateSignerUseCase.Params(
                name = name,
                xpub = signer.xpub,
                type = signer.type,
                derivationPath = signer.derivationPath,
                masterFingerprint = signer.masterFingerprint,
                tags = signer.tags,
                replace = replace,
            )
        ).onSuccess { createdSigner ->
            pushEventManager.push(PushEvent.LocalUserSignerAdded(createdSigner))
            _state.update {
                it.copy(
                    isProcessing = false,
                    pendingSigner = null,
                    existingKeyType = null,
                    replaceExistingKey = false,
                )
            }
            _event.emit(LedgerScanEvent.OpenSignerInfo(createdSigner))
        }.onFailure { e ->
            _state.update { it.copy(isProcessing = false) }
            _event.emit(LedgerScanEvent.Error(e.message.orUnknownError()))
        }
    }
}
