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
import com.nunchuk.android.core.domain.signer.SignMessageByTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Result
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.IsValidDerivationPathUseCase
import com.nunchuk.android.usecase.SendSignerPassphrase
import com.nunchuk.android.usecase.signer.GetHealthCheckPathUseCase
import com.nunchuk.android.usecase.signer.SignMessageBySoftwareKeyUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val args: SignMessageFragmentArgs =
        SignMessageFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _state = MutableStateFlow(SignMessageUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<SignMessageEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            getHealthCheckPathUseCase(Unit).onSuccess { path ->
                _state.update { it.copy(defaultPath = path) }
            }
        }
        if (args.signerType == SignerType.SOFTWARE) {
            viewModelScope.launch {
                getMasterSignerUseCase.invoke(args.masterSignerId)
                    .onSuccess { signer ->
                        _state.update { it.copy(needPassphrase = signer.device.needPassPhraseSent) }
                    }
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
            sendSignerPassphrase.execute(args.masterSignerId, passPhrase)
                .flowOn(Dispatchers.IO)
                .onException { _event.emit(SignMessageEvent.ShowError(it)) }
                .flowOn(Dispatchers.Main)
                .collect { signMessageBySoftware() }
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
            when (val result = createShareFileUseCase.execute("signature.txt")) {
                is Result.Success -> exportTransaction(
                    result.data,
                    state.value.signedMessage?.rfc2440.orEmpty()
                )

                is Result.Error -> _event.emit(SignMessageEvent.ShowError(result.exception))
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

    companion object {
        private const val KEY_MESSAGE = "a"
        private const val KEY_PATH = "b"
    }
}

sealed class SignMessageEvent {
    object InvalidPath : SignMessageEvent()
    object NoSignatureDetected : SignMessageEvent()
    object SignSuccess : SignMessageEvent()
    data class ShowError(val e: Throwable) : SignMessageEvent()
    data class ShareFile(val path: String) : SignMessageEvent()
    data class Loading(val isLoading: Boolean) : SignMessageEvent()
    data class NfcLoading(val isLoading: Boolean) : SignMessageEvent()
}