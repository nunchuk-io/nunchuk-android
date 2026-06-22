package com.nunchuk.android.signer.trezor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.utils.GetTrezorPublicKeyDeeplinkUseCase
import com.nunchuk.android.core.domain.utils.ParseTrezorPublicKeyResponseUseCase
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.TrezorCallbackMethod
import com.nunchuk.android.core.util.parseTrezorCallback
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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TREZOR_DEFAULT_SIGNER_NAME = "Trezor"

data class TrezorDeeplinkState(
    val isLoading: Boolean = false,
    val pendingSigner: SingleSigner? = null,
    val existingKeyType: ResultExistingKey? = null,
    val replaceExistingKey: Boolean = false,
    val defaultSignerName: String = TREZOR_DEFAULT_SIGNER_NAME,
    val lastHandledCallback: String = "",
)

sealed class TrezorDeeplinkEvent {
    data class OpenDeeplink(val url: String) : TrezorDeeplinkEvent()
    data object NavigateToSetKeyName : TrezorDeeplinkEvent()
    data class OpenSignerInfo(val signer: SingleSigner) : TrezorDeeplinkEvent()
    data class ShowError(val message: String) : TrezorDeeplinkEvent()
}

@HiltViewModel
class TrezorDeeplinkViewModel @Inject constructor(
    private val getTrezorPublicKeyDeeplinkUseCase: GetTrezorPublicKeyDeeplinkUseCase,
    private val parseTrezorPublicKeyResponseUseCase: ParseTrezorPublicKeyResponseUseCase,
    private val checkExistingKeyUseCase: CheckExistingKeyUseCase,
    private val createSignerUseCase: CreateSignerUseCase,
    private val pushEventManager: PushEventManager,
) : ViewModel() {
    private var isMembershipFlow: Boolean = false

    private val _state = MutableStateFlow(TrezorDeeplinkState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<TrezorDeeplinkEvent>()
    val event = _event.asSharedFlow()

    fun setMembershipFlow(value: Boolean) {
        isMembershipFlow = value
    }

    fun openTrezorSuiteDeeplink(
        walletType: WalletType,
        addressType: AddressType,
        index: Int
    ) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = false,
                    pendingSigner = null,
                    existingKeyType = null,
                    replaceExistingKey = false,
                    lastHandledCallback = ""
                )
            }
            getTrezorPublicKeyDeeplinkUseCase(
                GetTrezorPublicKeyDeeplinkUseCase.Param(
                    walletType = walletType,
                    addressType = addressType,
                    index = index
                )
            ).onSuccess { link ->
                _event.emit(TrezorDeeplinkEvent.OpenDeeplink(link))
            }.onFailure { e ->
                _event.emit(TrezorDeeplinkEvent.ShowError(e.message.orUnknownError()))
            }
        }
    }

    fun handleCallbackUri(callbackUri: String?): Boolean {
        val callback = parseTrezorCallback(callbackUri) ?: return false
        if (callback.method != TrezorCallbackMethod.GET_PUBLIC_KEY) return false
        if (_state.value.lastHandledCallback == callback.rawUri) return true
        if (callback.response.isBlank()) {
            _state.update { it.copy(lastHandledCallback = callback.rawUri) }
            return true
        }
        parseAndCheckExistingKey(response = callback.response, callback = callback.rawUri)
        return true
    }

    fun confirmExistingKeyDialog() {
        _state.update {
            it.copy(
                existingKeyType = null,
                replaceExistingKey = true
            )
        }
        if (isMembershipFlow) {
            createTrezorSigner(TREZOR_DEFAULT_SIGNER_NAME)
        } else {
            viewModelScope.launch {
                _event.emit(TrezorDeeplinkEvent.NavigateToSetKeyName)
            }
        }
    }

    fun dismissExistingKeyDialog() {
        _state.update {
            it.copy(
                pendingSigner = null,
                existingKeyType = null,
                replaceExistingKey = false
            )
        }
    }

    fun createTrezorSigner(name: String) {
        val signer = _state.value.pendingSigner ?: return
        val signerName = name.trim()
        if (signerName.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            createSignerUseCase(
                CreateSignerUseCase.Params(
                    name = signerName,
                    xpub = signer.xpub,
                    type = SignerType.HARDWARE,
                    derivationPath = signer.derivationPath,
                    masterFingerprint = signer.masterFingerprint.lowercase(),
                    publicKey = signer.publicKey,
                    tags = listOf(SignerTag.TREZOR),
                    replace = _state.value.replaceExistingKey
                )
            ).onSuccess { createdSigner ->
                pushEventManager.push(PushEvent.LocalUserSignerAdded(createdSigner))
                _state.update {
                    it.copy(
                        isLoading = false,
                        pendingSigner = null,
                        existingKeyType = null,
                        replaceExistingKey = false
                    )
                }
                _event.emit(TrezorDeeplinkEvent.OpenSignerInfo(createdSigner))
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false) }
                _event.emit(TrezorDeeplinkEvent.ShowError(e.message.orUnknownError()))
            }
        }
    }

    private fun parseAndCheckExistingKey(response: String, callback: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, lastHandledCallback = callback) }

            val signer = parseTrezorPublicKeyResponseUseCase(response).getOrElse { e ->
                _state.update { it.copy(isLoading = false) }
                _event.emit(TrezorDeeplinkEvent.ShowError(e.message.orUnknownError()))
                return@launch
            }

            val existingKeyType =
                checkExistingKeyUseCase(CheckExistingKeyUseCase.Params(singleSigner = signer))
                    .getOrElse { e ->
                        _state.update { it.copy(isLoading = false) }
                        _event.emit(TrezorDeeplinkEvent.ShowError(e.message.orUnknownError()))
                        return@launch
                    }

            if (existingKeyType == ResultExistingKey.None) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        pendingSigner = signer,
                        existingKeyType = null,
                        replaceExistingKey = false,
                    )
                }
                if (isMembershipFlow) {
                    createTrezorSigner(TREZOR_DEFAULT_SIGNER_NAME)
                } else {
                    _event.emit(TrezorDeeplinkEvent.NavigateToSetKeyName)
                }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        pendingSigner = signer,
                        existingKeyType = existingKeyType,
                        replaceExistingKey = false,
                    )
                }
            }
        }
    }
}
