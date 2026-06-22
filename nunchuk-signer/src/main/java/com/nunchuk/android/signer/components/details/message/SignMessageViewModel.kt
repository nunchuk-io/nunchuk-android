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
        loadDefaultPath()
        if (args.signerType == SignerType.SOFTWARE) {
            viewModelScope.launch {
                getMasterSignerUseCase.invoke(args.masterSignerId)
                    .onSuccess { signer ->
                        _state.update { it.copy(needPassphrase = signer.device.needPassPhraseSent) }
                    }
            }
        }
    }

    private fun loadDefaultPath() {
        viewModelScope.launch {
            if (isTrezorSigner()) {
                getRemoteSignerUseCase(
                    GetRemoteSignerUseCase.Data(
                        id = args.masterFingerprint,
                        derivationPath = args.derivationPath
                    )
                ).onSuccess { signer ->
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

    fun isTrezorSigner(): Boolean {
        return args.signerType == SignerType.HARDWARE
                && args.masterFingerprint.isNotBlank()
                && args.derivationPath.isNotBlank()
    }

    fun requestSignMessageByTrezor() {
        if (!isTrezorSigner()) return
        val message = savedStateHandle.get<String>(KEY_MESSAGE).orEmpty()
        if (message.isBlank()) return

        viewModelScope.launch {
            _event.emit(SignMessageEvent.Loading(true))
            getRemoteSignerUseCase(
                GetRemoteSignerUseCase.Data(
                    id = args.masterFingerprint,
                    derivationPath = args.derivationPath
                )
            ).onSuccess { signer ->
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
