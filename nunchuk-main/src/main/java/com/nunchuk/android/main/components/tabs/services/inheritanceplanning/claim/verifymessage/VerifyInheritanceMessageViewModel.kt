package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.data.model.membership.SigningChallengeMessage
import com.nunchuk.android.core.domain.membership.GetInheritanceClaimStateUseCase
import com.nunchuk.android.core.domain.signer.SignMessageByTapSignerUseCase
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.SignedMessage
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.SendSignerPassphraseUseCase
import com.nunchuk.android.usecase.signer.GenerateColdCardHealthCheckMessageStringUseCase
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
    private val getInheritanceClaimStateUseCase: GetInheritanceClaimStateUseCase,
    private val generateColdCardHealthCheckMessageStringUseCase: GenerateColdCardHealthCheckMessageStringUseCase,
    @Assisted private val signer: SignerModel,
    @Assisted private val challenge: SigningChallengeMessage
) : ViewModel() {
    private val message: String = challenge.message.orEmpty()
    private val messageId: String = challenge.id.orEmpty()

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
                }
            }.onFailure { error ->
                Timber.e(error, "Failed to sign message by TapSigner")
                _event.emit(VerifyInheritanceMessageEvent.NfcError(error))
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

    suspend fun generateColdCardSignedDataIfNeeded(): String {
        val currentState = _state.value
        if (!currentState.coldcardSignedData.isNullOrEmpty()) return currentState.coldcardSignedData
        return generateColdCardHealthCheckMessageStringUseCase(
            GenerateColdCardHealthCheckMessageStringUseCase.Param(
                derivationPath = signer.derivationPath,
                message = message,
                addressType = AddressType.NATIVE_SEGWIT
            )
        ).onSuccess { coldcardSignedData ->
            _state.update { it.copy(coldcardSignedData = coldcardSignedData) }
        }.getOrElse { error ->
            _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
            ""
        }
    }

    fun getInheritanceClaimState(magic: String) {
        viewModelScope.launch {
            val signedMessage = _state.value.signedMessage
            val signature = signedMessage?.signature.orEmpty()

            if (signature.isEmpty()) {
                _event.emit(VerifyInheritanceMessageEvent.ShowError("No signature available"))
                return@launch
            }

            _state.update { it.copy(loadingType = LoadingType.Normal) }

            getInheritanceClaimStateUseCase(
                GetInheritanceClaimStateUseCase.Param(
                    signerModels = listOf(signer),
                    signatures = listOf(signature),
                    magic = magic,
                    messageId = messageId
                )
            ).onSuccess { inheritanceAdditional ->
                _event.emit(
                    VerifyInheritanceMessageEvent.GetInheritanceClaimStateSuccess(
                        inheritanceAdditional
                    )
                )
            }.onFailure { error ->
                Timber.e(error, "Failed to get inheritance claim state")
                _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
            }
            _state.update { it.copy(loadingType = null) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            signer: SignerModel,
            challenge: SigningChallengeMessage
        ): VerifyInheritanceMessageViewModel
    }
}

data class VerifyInheritanceMessageUiState(
    val signedMessage: SignedMessage? = null,
    val needPassphrase: Boolean = false,
    val loadingType: LoadingType? = null,
    val coldcardSignedData: String? = null
)

enum class LoadingType {
    Normal, Nfc, ColdCard
}

sealed class VerifyInheritanceMessageEvent {
    object NoSignatureDetected : VerifyInheritanceMessageEvent()
    data class ShowError(val message: String) : VerifyInheritanceMessageEvent()
    data class NfcError(val e: Throwable) : VerifyInheritanceMessageEvent()
    data class GetInheritanceClaimStateSuccess(val inheritanceAdditional: InheritanceAdditional) :
        VerifyInheritanceMessageEvent()
}
