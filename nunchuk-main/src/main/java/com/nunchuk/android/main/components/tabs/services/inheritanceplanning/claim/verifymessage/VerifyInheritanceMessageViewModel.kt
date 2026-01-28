package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage

import android.content.Context
import android.net.Uri
import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.data.model.membership.SigningChallengeMessage
import com.nunchuk.android.core.domain.coldcard.SendDataToMk4UseCase
import com.nunchuk.android.core.domain.membership.GetInheritanceClaimStateUseCase
import com.nunchuk.android.core.domain.signer.SignMessageByTapSignerUseCase
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.getFileContentFromUri
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SignedMessage
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.SendSignerPassphraseUseCase
import com.nunchuk.android.usecase.signer.ExtractColdcardMessageSignatureUseCase
import com.nunchuk.android.usecase.signer.ExtractColdcardSignatureFromRecordsUseCase
import com.nunchuk.android.usecase.signer.GenerateColdCardHealthCheckMessageStringUseCase
import com.nunchuk.android.usecase.signer.SignMessageBySoftwareKeyUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileOutputStream

@HiltViewModel(assistedFactory = VerifyInheritanceMessageViewModel.Factory::class)
class VerifyInheritanceMessageViewModel @AssistedInject constructor(
    private val signMessageByTapSignerUseCase: SignMessageByTapSignerUseCase,
    private val signMessageBySoftwareKeyUseCase: SignMessageBySoftwareKeyUseCase,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val sendSignerPassphraseUseCase: SendSignerPassphraseUseCase,
    private val getInheritanceClaimStateUseCase: GetInheritanceClaimStateUseCase,
    private val generateColdCardHealthCheckMessageStringUseCase: GenerateColdCardHealthCheckMessageStringUseCase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val saveLocalFileUseCase: SaveLocalFileUseCase,
    private val sendDataToMk4UseCase: SendDataToMk4UseCase,
    private val extractColdcardMessageSignatureUseCase: ExtractColdcardMessageSignatureUseCase,
    private val extractColdcardSignatureFromRecordsUseCase: ExtractColdcardSignatureFromRecordsUseCase,
    @ApplicationContext private val applicationContext: Context,
    @IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
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
                addressType = AddressType.LEGACY
            )
        ).onSuccess { coldcardSignedData ->
            _state.update { it.copy(coldcardSignedData = coldcardSignedData) }
        }.getOrElse { error ->
            _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
            ""
        }
    }

    fun getInheritanceClaimState(
        magic: String,
        signers: Set<SignerModel>,
        signatures: List<String>
    ) {
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
                    signerModels = signers.toList(),
                    signatures = signatures,
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

    fun exportTransactionToFile(dataToSign: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadingType = LoadingType.Normal) }
            when (val result = createShareFileUseCase.execute("coldcard_message.txt")) {
                is Result.Success -> exportTransaction(result.data, dataToSign)
                is Result.Error -> {
                    _event.emit(VerifyInheritanceMessageEvent.ShowError(result.exception.messageOrUnknownError()))
                    _state.update { it.copy(loadingType = null) }
                }
            }
        }
    }

    private fun exportTransaction(filePath: String, dataToSign: String) {
        viewModelScope.launch {
            val result = runCatching {
                FileOutputStream(filePath).use {
                    it.write(dataToSign.toByteArray(Charsets.UTF_8))
                }
            }
            _state.update { it.copy(loadingType = null) }
            if (result.isSuccess) {
                _event.emit(VerifyInheritanceMessageEvent.ExportToFileSuccess(filePath))
            } else {
                _event.emit(VerifyInheritanceMessageEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun saveLocalFile(dataToSign: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadingType = LoadingType.Normal) }
            val result = saveLocalFileUseCase(
                SaveLocalFileUseCase.Params(
                    fileName = "coldcard_message.txt",
                    fileContent = dataToSign
                )
            )
            _state.update { it.copy(loadingType = null) }
            _event.emit(VerifyInheritanceMessageEvent.SaveLocalFile(result.isSuccess))
        }
    }

    fun handleExportTransactionToMk4(ndef: Ndef) {
        viewModelScope.launch {
            generateColdCardSignedDataIfNeeded()
            val coldcardSignedData = _state.value.coldcardSignedData
            if (!coldcardSignedData.isNullOrEmpty()) {
                exportToMk4(coldcardSignedData, ndef)
            }
        }
    }

    private fun exportToMk4(dataToSign: String, ndef: Ndef) {
        viewModelScope.launch {
            _state.update { it.copy(loadingType = LoadingType.ColdCard) }
            val result = sendDataToMk4UseCase(
                SendDataToMk4UseCase.Data(dataToSign, ndef)
            )
            _state.update { it.copy(loadingType = null) }
            if (result.isSuccess) {
                _event.emit(VerifyInheritanceMessageEvent.ExportTransactionToColdcardSuccess)
            } else {
                _event.emit(VerifyInheritanceMessageEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun importSignatureFromFile(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(loadingType = LoadingType.Normal) }
            try {
                val fileContent = withContext(ioDispatcher) {
                    getFileContentFromUri(applicationContext.contentResolver, uri)
                } ?: throw Exception("Failed to read file content")

                val signature = extractColdcardMessageSignatureUseCase(fileContent).getOrThrow()
                _state.update {
                    it.copy(
                        signedMessage = SignedMessage(
                            signature = signature,
                        ),
                    )
                }
            } catch (e: Exception) {
                _event.emit(VerifyInheritanceMessageEvent.ShowError(e.message.orUnknownError()))
            }
            _state.update { it.copy(loadingType = null) }
        }
    }

    fun importSignature(signature: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadingType = LoadingType.Normal) }
            try {
                _state.update {
                    it.copy(
                        signedMessage = SignedMessage(
                            signature = signature,
                        ),
                    )
                }
            } catch (e: Exception) {
                _event.emit(VerifyInheritanceMessageEvent.ShowError(e.message.orUnknownError()))
            }
            _state.update { it.copy(loadingType = null) }
        }
    }

    fun importSignatureFromNfc(records: List<NdefRecord>) {
        viewModelScope.launch {
            _state.update { it.copy(loadingType = LoadingType.ColdCard) }
            extractColdcardSignatureFromRecordsUseCase(records.toTypedArray())
                .onSuccess { signature ->
                    _state.update {
                        it.copy(
                            signedMessage = SignedMessage(
                                signature = signature,
                            ),
                        )
                    }
                }
                .onFailure { e ->
                    _event.emit(VerifyInheritanceMessageEvent.ShowError(e.message.orUnknownError()))
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

    data class ExportToFileSuccess(val filePath: String) : VerifyInheritanceMessageEvent()
    data class SaveLocalFile(val isSuccess: Boolean) : VerifyInheritanceMessageEvent()
    object ExportTransactionToColdcardSuccess : VerifyInheritanceMessageEvent()
}
