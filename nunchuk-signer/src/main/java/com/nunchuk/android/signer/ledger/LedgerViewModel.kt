package com.nunchuk.android.signer.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.utils.GetBip32PathUseCase
import com.nunchuk.android.core.ledger.LedgerDevice
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CheckExistingKeyUseCase
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.ResultExistingKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    // Set when the fetched xpub matches a key already in the app: the signer waits on the
    // replace-key confirmation before being persisted.
    val pendingSigner: SingleSigner? = null,
    val existingKeyType: ResultExistingKey? = null,
) {
    val selectedDevice: LedgerDevice?
        get() = devices.firstOrNull { it.id == selectedAddress }
}

sealed class LedgerScanEvent {
    data class OpenSignerInfo(val signer: SingleSigner) : LedgerScanEvent()

    data class Error(val message: String) : LedgerScanEvent()
}

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val createSignerUseCase: CreateSignerUseCase,
    private val getBip32PathUseCase: GetBip32PathUseCase,
    private val checkExistingKeyUseCase: CheckExistingKeyUseCase,
    private val pushEventManager: PushEventManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LedgerScanUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<LedgerScanEvent>()
    val event = _event.asSharedFlow()

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
     * + extended public key, tagged as a Ledger hardware signer. Persists it straight away
     * unless the key already exists in the app, in which case the UI asks to replace it first.
     */
    fun onXpubReceived(
        name: String,
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
                name = name,
                xpub = xpub,
                derivationPath = path,
                masterFingerprint = masterFingerprint.lowercase(),
                type = SignerType.HARDWARE,
                tags = listOf(SignerTag.LEDGER),
            )
            checkExistingKeyUseCase(CheckExistingKeyUseCase.Params(singleSigner = signer))
                .onSuccess { existingKeyType ->
                    if (existingKeyType == ResultExistingKey.None) {
                        createSigner(signer, replace = false)
                    } else {
                        _state.update {
                            it.copy(
                                isProcessing = false,
                                pendingSigner = signer,
                                existingKeyType = existingKeyType,
                            )
                        }
                    }
                }.onFailure { e -> onError(e.message.orUnknownError()) }
        }.onFailure { e -> onError(e.message.orUnknownError()) }
    }

    fun confirmExistingKeyDialog() {
        val signer = _state.value.pendingSigner ?: return
        _state.update { it.copy(pendingSigner = null, existingKeyType = null) }
        viewModelScope.launch { createSigner(signer, replace = true) }
    }

    fun dismissExistingKeyDialog() = _state.update {
        it.copy(pendingSigner = null, existingKeyType = null)
    }

    private suspend fun createSigner(signer: SingleSigner, replace: Boolean) {
        _state.update { it.copy(isProcessing = true) }
        createSignerUseCase(
            CreateSignerUseCase.Params(
                name = signer.name,
                xpub = signer.xpub,
                type = signer.type,
                derivationPath = signer.derivationPath,
                masterFingerprint = signer.masterFingerprint,
                tags = signer.tags,
                replace = replace,
            )
        ).onSuccess { createdSigner ->
            pushEventManager.push(PushEvent.LocalUserSignerAdded(createdSigner))
            _state.update { it.copy(isProcessing = false) }
            _event.emit(LedgerScanEvent.OpenSignerInfo(createdSigner))
        }.onFailure { e ->
            _state.update { it.copy(isProcessing = false) }
            _event.emit(LedgerScanEvent.Error(e.message.orUnknownError()))
        }
    }
}
