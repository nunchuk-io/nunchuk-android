package com.nunchuk.android.signer.components.details.message

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.signer.SignMessageByTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.IsValidDerivationPathUseCase
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
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val savedStateHandle: SavedStateHandle,
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

    fun exportSignatureToFile() {
        viewModelScope.launch {
            _event.emit(SignMessageEvent.Loading(true))
            when (val result = createShareFileUseCase.execute( "signature.txt")) {
                is Result.Success -> exportTransaction(result.data, state.value.signedMessage?.rfc2440.orEmpty())
                is Result.Error -> _event.emit(SignMessageEvent.ShowError(result.exception))
            }
            _event.emit(SignMessageEvent.Loading(false))
        }
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