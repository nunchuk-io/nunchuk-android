/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.signer.components.details.message

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.utils.GetBitBoxSignMessagePathUseCase
import com.nunchuk.android.core.domain.utils.GetBitBoxSignedMessageUseCase
import com.nunchuk.android.core.domain.utils.GetSignedMessageUseCase
import com.nunchuk.android.core.domain.utils.GetTrezorSignMessagePathUseCase
import com.nunchuk.android.core.domain.utils.GetTrezorSignMessageDeeplinkUseCase
import com.nunchuk.android.core.domain.utils.ParseTrezorSignMessageResponseUseCase
import com.nunchuk.android.core.util.TrezorCallbackMethod
import com.nunchuk.android.core.util.parseTrezorCallback
import com.nunchuk.android.core.domain.signer.SignMessageByTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import com.nunchuk.android.usecase.IsValidDerivationPathUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.SendSignerPassphraseUseCase
import com.nunchuk.android.usecase.signer.GetHealthCheckPathUseCase
import com.nunchuk.android.usecase.signer.SignMessageBySoftwareKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class SignMessageViewModel @Inject constructor(
    private val getHealthCheckPathUseCase: GetHealthCheckPathUseCase,
    private val isValidDerivationPathUseCase: IsValidDerivationPathUseCase,
    private val signMessageByTapSignerUseCase: SignMessageByTapSignerUseCase,
    private val signMessageBySoftwareKeyUseCase: SignMessageBySoftwareKeyUseCase,
    private val getTrezorSignMessagePathUseCase: GetTrezorSignMessagePathUseCase,
    private val getTrezorSignMessageDeeplinkUseCase: GetTrezorSignMessageDeeplinkUseCase,
    private val getSignedMessageUseCase: GetSignedMessageUseCase,
    private val getBitBoxSignMessagePathUseCase: GetBitBoxSignMessagePathUseCase,
    private val getBitBoxSignedMessageUseCase: GetBitBoxSignedMessageUseCase,
    private val parseTrezorSignMessageResponseUseCase: ParseTrezorSignMessageResponseUseCase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val sendSignerPassphraseUseCase: SendSignerPassphraseUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val saveLocalFileUseCase: SaveLocalFileUseCase
) : ViewModel() {
    private val args: SignMessageFragmentArgs =
        SignMessageFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _state = MutableStateFlow(SignMessageUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<SignMessageEvent>()
    val event = _event.asSharedFlow()
    private var lastHandledTrezorCallback: String = ""

    init {
        loadSigner()
        if (args.signerType == SignerType.SOFTWARE) {
            viewModelScope.launch {
                getMasterSignerUseCase.invoke(args.masterSignerId)
                    .onSuccess { signer ->
                        _state.update { it.copy(needPassphrase = signer.device.needPassPhraseSent) }
                    }
            }
        }
    }

    /**
     * Loads the hardware signer (its tags decide how signing happens: Trezor Suite deeplink or
     * Ledger and BitBox over BLE/USB) and the derivation path to sign with.
     */
    private fun loadSigner() {
        viewModelScope.launch {
            if (isHardwareSigner()) {
                getRemoteSignerUseCase(
                    GetRemoteSignerUseCase.Data(
                        id = args.masterFingerprint,
                        derivationPath = args.derivationPath
                    )
                ).onSuccess { signer ->
                    _state.update { it.copy(remoteSigner = signer) }
                    // A Ledger signs with the key at the signer's own path (Confluence "3. Sign
                    // message"), which is also the path its address is derived from.
                    if (isLedgerSigner()) {
                        _state.update { it.copy(defaultPath = signer.derivationPath) }
                        return@launch
                    }
                    // A BitBox signs with the compact-signature key *below* the signer
                    // (Confluence "4. Sign message"), which the SDK resolves. There is no sane
                    // fallback: a guessed path signs with the wrong key and the export then
                    // verifies against nothing, so leave the path empty and let signing refuse.
                    if (isBitBoxSigner()) {
                        getBitBoxSignMessagePathUseCase(
                            GetBitBoxSignMessagePathUseCase.Param(signer = signer)
                        ).onSuccess { path ->
                            _state.update { it.copy(defaultPath = path) }
                        }.onFailure {
                            _event.emit(SignMessageEvent.ShowError(it))
                        }
                        return@launch
                    }
                    getTrezorSignMessagePathUseCase(
                        GetTrezorSignMessagePathUseCase.Param(signer = signer)
                    ).onSuccess { path ->
                        if (path.isNotBlank()) {
                            _state.update { it.copy(defaultPath = path) }
                            return@launch
                        }
                    }
                }
            }

            getHealthCheckPathUseCase(Unit).onSuccess { path ->
                _state.update { it.copy(defaultPath = path) }
            }
        }
    }

    fun validatePath(path: String) {
        viewModelScope.launch {
            isValidDerivationPathUseCase(path).onSuccess { isValid ->
                if (isValid.not()) {
                    _event.emit(SignMessageEvent.InvalidPath)
                }
            }.onFailure {
                _event.emit(SignMessageEvent.InvalidPath)
            }
        }
    }

    fun saveMessage(message: String, path: String) {
        savedStateHandle[KEY_MESSAGE] = message.trim()
        savedStateHandle[KEY_PATH] = path.trim()
    }

    private fun isHardwareSigner(): Boolean {
        return args.signerType == SignerType.HARDWARE
                && args.masterFingerprint.isNotBlank()
                && args.derivationPath.isNotBlank()
    }

    fun isTrezorSigner(): Boolean = isHardwareSigner() && _state.value.isTrezor

    fun isLedgerSigner(): Boolean = isHardwareSigner() && _state.value.isLedger

    fun isBitBoxSigner(): Boolean = isHardwareSigner() && _state.value.isBitBox

    fun requestSignMessageByTrezor() {
        if (!isTrezorSigner()) return
        val message = savedStateHandle.get<String>(KEY_MESSAGE).orEmpty()
        if (message.isBlank()) return
        val signer = _state.value.remoteSigner ?: return

        viewModelScope.launch {
            _event.emit(SignMessageEvent.Loading(true))
            getTrezorSignMessageDeeplinkUseCase(
                GetTrezorSignMessageDeeplinkUseCase.Param(
                    signer = signer,
                    message = message
                )
            ).onSuccess { deeplink ->
                _event.emit(SignMessageEvent.ShowOpenTrezorSuiteConfirmation(deeplink))
            }.onFailure {
                _event.emit(SignMessageEvent.ShowError(it))
            }
            _event.emit(SignMessageEvent.Loading(false))
        }
    }

    /**
     * The BitBox signed the message over BLE/USB. Unlike Ledger, the export can't be built from
     * the signer: the device signed with the compact-signature key below it, so the block has to
     * name *that* key's address or it verifies against nothing (Confluence "4. Sign message").
     */
    fun onBitBoxMessageSigned(signature: String) {
        val signer = _state.value.remoteSigner ?: return
        viewModelScope.launch {
            _event.emit(SignMessageEvent.Loading(true))
            getBitBoxSignedMessageUseCase(
                GetBitBoxSignedMessageUseCase.Param(
                    signer = signer,
                    message = savedStateHandle.get<String>(KEY_MESSAGE).orEmpty(),
                    signature = signature,
                )
            ).onSuccess { signedMessage ->
                _state.update { it.copy(signedMessage = signedMessage) }
                if (signedMessage.signature.isBlank()) {
                    _event.emit(SignMessageEvent.NoSignatureDetected)
                } else {
                    _event.emit(SignMessageEvent.SignSuccess)
                }
            }.onFailure {
                _event.emit(SignMessageEvent.ShowError(it))
            }
            _event.emit(SignMessageEvent.Loading(false))
        }
    }

    /**
     * The Ledger signed the message over BLE/USB; pair its signature with the signer's address
     * to get the same signed-message export the in-app signers produce.
     */
    fun onLedgerMessageSigned(signature: String) {
        val signer = _state.value.remoteSigner ?: return
        viewModelScope.launch {
            _event.emit(SignMessageEvent.Loading(true))
            getSignedMessageUseCase(
                GetSignedMessageUseCase.Param(
                    signer = signer,
                    message = savedStateHandle.get<String>(KEY_MESSAGE).orEmpty(),
                    signature = signature
                )
            ).onSuccess { signedMessage ->
                _state.update { it.copy(signedMessage = signedMessage) }
                if (signedMessage.signature.isBlank()) {
                    _event.emit(SignMessageEvent.NoSignatureDetected)
                } else {
                    _event.emit(SignMessageEvent.SignSuccess)
                }
            }.onFailure {
                _event.emit(SignMessageEvent.ShowError(it))
            }
            _event.emit(SignMessageEvent.Loading(false))
        }
    }

    fun handleTrezorCallback(callbackUri: String?): Boolean {
        val callback = parseTrezorCallback(callbackUri) ?: return false
        if (callback.method != TrezorCallbackMethod.SIGN_MESSAGE) return false
        if (lastHandledTrezorCallback == callback.rawUri) return true
        lastHandledTrezorCallback = callback.rawUri
        if (callback.response.isBlank()) return true

        viewModelScope.launch {
            _event.emit(SignMessageEvent.Loading(true))
            parseTrezorSignMessageResponseUseCase(
                ParseTrezorSignMessageResponseUseCase.Param(
                    response = callback.response,
                    message = callback.message.ifBlank {
                        savedStateHandle.get<String>(KEY_MESSAGE).orEmpty()
                    }
                )
            ).onSuccess { signedMessage ->
                _state.update { it.copy(signedMessage = signedMessage) }
                if (signedMessage.signature.isBlank()) {
                    _event.emit(SignMessageEvent.NoSignatureDetected)
                } else {
                    _event.emit(SignMessageEvent.SignSuccess)
                }
            }.onFailure {
                _event.emit(SignMessageEvent.ShowError(it))
            }
            _event.emit(SignMessageEvent.Loading(false))
        }
        return true
    }

    fun signMessageByTapSigner(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            _event.emit(SignMessageEvent.NfcLoading(true))
            signMessageByTapSignerUseCase(
                SignMessageByTapSignerUseCase.Data(
                    isoDep = isoDep,
                    cvc = cvc,
                    message = savedStateHandle.get<String>(KEY_MESSAGE).orEmpty(),
                    path = savedStateHandle.get<String>(KEY_PATH).orEmpty(),
                    masterSignerId = args.masterSignerId
                )
            ).onSuccess { signedMessage ->
                _state.update { it.copy(signedMessage = signedMessage) }
                if (signedMessage?.signature.isNullOrEmpty()) {
                    _event.emit(SignMessageEvent.NoSignatureDetected)
                } else {
                    _event.emit(SignMessageEvent.SignSuccess)
                }
            }.onFailure {
                _event.emit(SignMessageEvent.ShowError(it))
            }
            _event.emit(SignMessageEvent.NfcLoading(false))
        }
    }

    fun handleHealthCheck(passPhrase: String) {
        viewModelScope.launch {
            sendSignerPassphraseUseCase(
                SendSignerPassphraseUseCase.Param(
                    signerId = args.masterSignerId,
                    passphrase = passPhrase
                )
            ).onSuccess {
                signMessageBySoftware()
            }.onFailure { exception ->
                _event.emit(SignMessageEvent.ShowError(exception))
            }
        }
    }

    fun signMessageBySoftware() {
        viewModelScope.launch {
            _event.emit(SignMessageEvent.Loading(true))
            signMessageBySoftwareKeyUseCase(
                SignMessageBySoftwareKeyUseCase.Data(
                    message = savedStateHandle.get<String>(KEY_MESSAGE).orEmpty(),
                    path = savedStateHandle.get<String>(KEY_PATH).orEmpty(),
                    masterSignerId = args.masterSignerId
                )
            ).onSuccess { signedMessage ->
                _state.update { it.copy(signedMessage = signedMessage) }
                if (signedMessage?.signature.isNullOrEmpty()) {
                    _event.emit(SignMessageEvent.NoSignatureDetected)
                } else {
                    _event.emit(SignMessageEvent.SignSuccess)
                }
            }.onFailure {
                _event.emit(SignMessageEvent.ShowError(it))
            }
            _event.emit(SignMessageEvent.Loading(false))
        }
    }

    fun needPassphrase() = _state.value.needPassphrase

    fun exportSignatureToFile() {
        viewModelScope.launch {
            _event.emit(SignMessageEvent.Loading(true))
            createShareFileUseCase("signature.txt").onSuccess { filePath ->
                exportTransaction(
                    filePath,
                    state.value.signedMessage?.rfc2440.orEmpty()
                )
            }.onFailure {
                _event.emit(SignMessageEvent.ShowError(it))
            }
            _event.emit(SignMessageEvent.Loading(false))
        }
    }

    fun resetSignature() {
        _state.update { it.copy(signedMessage = null) }
    }

    private suspend fun exportTransaction(filePath: String, signature: String) {
        runCatching {
            withContext(ioDispatcher) {
                FileOutputStream(filePath).use {
                    it.write(signature.toByteArray(Charsets.UTF_8))
                }
            }
        }.onSuccess {
            _event.emit(SignMessageEvent.ShareFile(filePath))
        }.onFailure { e ->
            _event.emit(SignMessageEvent.ShowError(e))
        }
    }

    fun saveLocalFile() {
        viewModelScope.launch {
            val result = saveLocalFileUseCase(SaveLocalFileUseCase.Params(fileName = "signature.txt", fileContent = state.value.signedMessage?.rfc2440.orEmpty()))
            _event.emit(SignMessageEvent.SaveLocalFile(result.isSuccess))
        }
    }

    companion object {
        private const val KEY_MESSAGE = "a"
        private const val KEY_PATH = "b"
    }
}

sealed class SignMessageEvent {
    object InvalidPath : SignMessageEvent()
    object NoSignatureDetected : SignMessageEvent()
    object SignSuccess : SignMessageEvent()
    data class ShowOpenTrezorSuiteConfirmation(val deeplink: String) : SignMessageEvent()
    data class ShowError(val e: Throwable) : SignMessageEvent()
    data class ShareFile(val path: String) : SignMessageEvent()
    data class Loading(val isLoading: Boolean) : SignMessageEvent()
    data class NfcLoading(val isLoading: Boolean) : SignMessageEvent()
    data class SaveLocalFile(val isSuccess: Boolean) : SignMessageEvent()
}
