package com.nunchuk.android.signer.portal

import android.nfc.tech.NfcA
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.utils.GetBip32PathUseCase
import com.nunchuk.android.core.domain.utils.ParseSignerStringUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateSignerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.twenty_two.CardStatus
import xyz.twenty_two.GenerateMnemonicWords
import xyz.twenty_two.PortalSdk
import javax.inject.Inject

@HiltViewModel
class PortalDeviceViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getBip32PathUseCase: GetBip32PathUseCase,
    private val parseSignerStringUseCase: ParseSignerStringUseCase,
    private val createSignerUseCase: CreateSignerUseCase
) : ViewModel() {
    private val sdk = PortalSdk(true)
    private val _state = MutableStateFlow(PortalDeviceUiState())
    val state = _state.asStateFlow()

    private var sdkJob: Job? = null

    fun setPendingAction(action: PortalAction) {
        savedStateHandle[EXTRA_PENDING_ACTION] = action
        if (action is PortalActionWithPin) {
            _state.update { state -> state.copy(event = PortalDeviceEvent.AskPin) }
        } else if (!isConnectedToSdk) {
            _state.update { state -> state.copy(event = PortalDeviceEvent.RequestScan) }
        } else {
            executeAction(action)
        }
    }

    private fun executeAction(newAction: PortalAction? = null) {
        val action = newAction ?: savedStateHandle.get<PortalAction>(EXTRA_PENDING_ACTION) ?: return
        viewModelScope.launch {
            _state.update { state -> state.copy(isLoading = true) }
            runCatching {
                when (action) {
                    AddNewPortal -> getStatus()

                    is SetupPortal -> setupPortal(action)
                    is GetXpub -> createSigner()

                    else -> Unit
                }
            }.onFailure {
                Timber.e(it)
                _state.update { state -> state.copy(message = it.message.orEmpty()) }
            }
            _state.update { state -> state.copy(isLoading = false) }
        }
        savedStateHandle.remove<PortalAction>(EXTRA_PENDING_ACTION)
    }

    private suspend fun createSigner() {
        val status = sdk.getStatus()
        val index = savedStateHandle.get<Int>(EXTRA_INDEX) ?: 0
        val walletType =
            if (savedStateHandle.get<Boolean>(EXTRA_MULTISIG) == true) WalletType.MULTI_SIG else WalletType.SINGLE_SIG
        val addressType = AddressType.NATIVE_SEGWIT
        val path =
            getBip32PathUseCase(GetBip32PathUseCase.Param(index, walletType, addressType)).getOrThrow()
        if (!status.unlocked) {
            runCatching {
                sdk.unlock(savedStateHandle.get<String>(EXTRA_PIN).orEmpty())
            }.onSuccess {
                createSigner(path)
            }.onFailure {
                _state.update { state -> state.copy(event = PortalDeviceEvent.IncorrectPin) }
            }
        } else {
            createSigner(path)
        }
    }

    private suspend fun createSigner(path: String) {
        val xpub = sdk.getXpub(path).xpub
        val signer = parseSignerStringUseCase(xpub).getOrNull() ?: return
        createSignerUseCase(
            CreateSignerUseCase.Params(
                xpub = signer.xpub,
                derivationPath = signer.derivationPath,
                name = savedStateHandle.get<String>(EXTRA_NAME).orEmpty(),
                masterFingerprint = signer.masterFingerprint,
                type = SignerType.PORTAL_NFC,
                publicKey = signer.publicKey,
            )
        ).onSuccess {
            _state.update { state -> state.copy(event = PortalDeviceEvent.OpenSignerInfo(it)) }
        }.onFailure {
            Timber.e(it)
            _state.update { state -> state.copy(message = it.message.orEmpty()) }
        }
    }

    private suspend fun getStatus() {
        val status = sdk.getStatus()
        val newStatus = if (!status.initialized && status.unverified != null) {
            sdk.resume()
            sdk.getStatus()
        } else {
            status
        }

        _state.update { state ->
            state.copy(event = PortalDeviceEvent.AddPortal(newStatus))
        }
    }

    private suspend fun setupPortal(action: SetupPortal) {
        val chain =
            getAppSettingUseCase(Unit).getOrThrow().chain.toPortalNetwork()
        if (action.mnemonic.isNotEmpty()) {
            sdk.restoreMnemonic(
                mnemonic = action.mnemonic,
                network = chain,
                password = action.pin
            )
        } else {
            val numWords =
                if (action.numberOfWords == 12) GenerateMnemonicWords.WORDS12 else GenerateMnemonicWords.WORDS24
            sdk.generateMnemonic(
                numWords = numWords,
                network = chain,
                password = action.pin
            )
        }
        _state.update { state -> state.copy(event = PortalDeviceEvent.StartSetupWallet) }
    }

    fun newTag(newTag: NfcA) {
        runCatching { _state.value.tag?.close() }
        newTag.timeout = 5000
        if (!newTag.isConnected) {
            newTag.connect()
        }
        _state.update { state -> state.copy(tag = newTag, isConnected = false) }
        sdkJob?.cancel()
        sdkJob = viewModelScope.launch(ioDispatcher) {
            runCatching {
                sdk.newTag()
            }.onSuccess {
                _state.update { state -> state.copy(isConnected = true) }
                executeAction()
                while (isActive) {
                    val msg = sdk.poll()
                    try {
                        val resp = newTag.transceive(msg.data)
                        sdk.incomingData(msg.msgIndex, resp!!)
                    } catch (e: Exception) {
                        _state.update { state -> state.copy(isConnected = false) }
                        break
                    }
                }
            }

        }
    }

    fun updateIndex(index: Int) {
        savedStateHandle[EXTRA_INDEX] = index
    }

    fun updateMultisig(isMultisig: Boolean) {
        savedStateHandle[EXTRA_MULTISIG] = isMultisig
    }

    fun updatePin(pin: String) {
        savedStateHandle[EXTRA_PIN] = pin
        executeAction()
    }

    fun updateName(name: String) {
        savedStateHandle[EXTRA_NAME] = name
    }

    fun markEventHandled() {
        _state.update { state -> state.copy(event = null) }
    }

    fun markMessageHandled() {
        _state.update { state -> state.copy(message = "") }
    }

    fun hideLoading() {
        _state.update { state -> state.copy(isLoading = false) }
    }

    val isConnectedToSdk: Boolean
        get() = state.value.isConnected && runCatching { state.value.tag?.isConnected == true }.getOrElse { false }


    override fun onCleared() {
        runCatching { _state.value.tag?.close() }
        sdk.destroy()
        super.onCleared()
    }

    companion object {
        const val EXTRA_MULTISIG = "is_multisig"
        const val EXTRA_INDEX = "index"
        const val EXTRA_PENDING_ACTION = "pending_action"
        const val EXTRA_PIN = "pin"
        const val EXTRA_NAME = "name"
    }
}

sealed class PortalDeviceEvent {
    data class AddPortal(val status: CardStatus) : PortalDeviceEvent()
    data object RequestScan : PortalDeviceEvent()
    data object AskPin : PortalDeviceEvent()
    data object StartSetupWallet : PortalDeviceEvent()
    data object IncorrectPin : PortalDeviceEvent()
    data class OpenSignerInfo(val signer: SingleSigner) : PortalDeviceEvent()
}