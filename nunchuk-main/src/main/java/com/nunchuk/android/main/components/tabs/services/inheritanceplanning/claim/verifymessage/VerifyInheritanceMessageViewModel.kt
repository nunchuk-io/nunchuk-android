package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.signer.SignMessageByTapSignerUseCase
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SignedMessage
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.SendSignerPassphraseUseCase
import com.nunchuk.android.usecase.signer.SignMessageBySoftwareKeyUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel(assistedFactory = VerifyInheritanceMessageViewModel.Factory::class)
class VerifyInheritanceMessageViewModel @AssistedInject constructor(
    private val signMessageByTapSignerUseCase: SignMessageByTapSignerUseCase,
    private val signMessageBySoftwareKeyUseCase: SignMessageBySoftwareKeyUseCase,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val sendSignerPassphraseUseCase: SendSignerPassphraseUseCase,
    @Assisted private val signer: SignerModel,
    @Assisted private val message: String
) : ViewModel() {

    private val _state = MutableStateFlow(VerifyInheritanceMessageUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<VerifyInheritanceMessageEvent>()
    val event = _event.asSharedFlow()

    init {
        if (signer.type == SignerType.SOFTWARE) {
            viewModelScope.launch {
                getMasterSignerUseCase.invoke(signer.id)
                    .onSuccess { masterSigner ->
                        _state.update {
                            it.copy(needPassphrase = masterSigner.device.needPassPhraseSent)
                        }
                    }
                    .onFailure { e ->
                        Timber.e(e, "Failed to get master signer for passphrase check")
                    }
            }
        }
    }

    fun signMessageByTapSigner(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            val path = signer.derivationPath
            val masterSignerId = signer.fingerPrint

            _state.update { it.copy(loadingType = LoadingType.Nfc) }
            signMessageByTapSignerUseCase(
                SignMessageByTapSignerUseCase.Data(
                    isoDep = isoDep,
                    cvc = cvc,
                    message = message,
                    path = path,
                    masterSignerId = masterSignerId
                )
            ).onSuccess { signedMessage ->
                _state.update { it.copy(signedMessage = signedMessage) }
                if (signedMessage?.signature.isNullOrEmpty()) {
                    _event.emit(VerifyInheritanceMessageEvent.NoSignatureDetected)
                } else {
                    _event.emit(VerifyInheritanceMessageEvent.SignSuccess)
                }
            }.onFailure { error ->
                Timber.e(error, "Failed to sign message by TapSigner")
                _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
            }
            _state.update { it.copy(loadingType = null) }
        }
    }

    fun handlePassphrase(passphrase: String) {
        viewModelScope.launch {
            val masterSignerId = signer.fingerPrint

            _state.update { it.copy(loadingType = LoadingType.Normal) }
            sendSignerPassphraseUseCase(
                SendSignerPassphraseUseCase.Param(
                    signerId = masterSignerId,
                    passphrase = passphrase
                )
            ).onSuccess {
                signMessageBySoftware()
            }.onFailure { error ->
                Timber.e(error, "Failed to send passphrase")
                _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
            }
            _state.update { it.copy(loadingType = null) }
        }
    }

    fun signMessageBySoftware() {
        viewModelScope.launch {
            val path = signer.derivationPath
            val masterSignerId = signer.fingerPrint

            _state.update { it.copy(loadingType = LoadingType.Normal) }
            signMessageBySoftwareKeyUseCase(
                SignMessageBySoftwareKeyUseCase.Data(
                    message = message,
                    path = path,
                    masterSignerId = masterSignerId
                )
            ).onSuccess { signedMessage ->
                _state.update { it.copy(signedMessage = signedMessage) }
                if (signedMessage?.signature.isNullOrEmpty()) {
                    _event.emit(VerifyInheritanceMessageEvent.NoSignatureDetected)
                } else {
                    _event.emit(VerifyInheritanceMessageEvent.SignSuccess)
                }
            }.onFailure { error ->
                Timber.e(error, "Failed to sign message by software")
                _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
            }
            _state.update { it.copy(loadingType = null) }
        }
    }

    fun needPassphrase(): Boolean = _state.value.needPassphrase

    fun resetSignature() {
        _state.update { it.copy(signedMessage = null) }
    }

    @AssistedFactory
    interface Factory {
        fun create(signer: SignerModel, message: String): VerifyInheritanceMessageViewModel
    }
}

data class VerifyInheritanceMessageUiState(
    val signedMessage: SignedMessage? = null,
    val needPassphrase: Boolean = false,
    val loadingType: LoadingType? = null
)

enum class LoadingType {
    Normal, Nfc, ColdCard
}

sealed class VerifyInheritanceMessageEvent {
    object NoSignatureDetected : VerifyInheritanceMessageEvent()
    object SignSuccess : VerifyInheritanceMessageEvent()
    data class ShowError(val message: String) : VerifyInheritanceMessageEvent()
}
