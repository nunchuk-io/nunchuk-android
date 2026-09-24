package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage

import android.content.Context
import android.net.Uri
import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.constants.NativeErrorCode
import com.nunchuk.android.core.data.model.membership.SigningChallengeMessage
import com.nunchuk.android.core.domain.coldcard.SendDataToMk4UseCase
import com.nunchuk.android.core.domain.membership.GetInheritanceClaimStateUseCase
import com.nunchuk.android.core.domain.utils.GetBitBoxSignMessagePathUseCase
import com.nunchuk.android.core.domain.utils.GetTrezorSignMessageDeeplinkUseCase
import com.nunchuk.android.core.domain.utils.ParseTrezorSignMessageResponseUseCase
import com.nunchuk.android.core.domain.signer.SignMessageByTapSignerUseCase
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.getFileContentFromUri
import com.nunchuk.android.core.util.nativeErrorCode
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.TrezorCallbackHolder
import com.nunchuk.android.core.util.TrezorCallbackMethod
import com.nunchuk.android.core.util.parseTrezorCallback
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.SignedMessage
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.SendSignerPassphraseUseCase
import com.nunchuk.android.usecase.signer.ExtractMessageSignatureUseCase
import com.nunchuk.android.usecase.signer.ExtractColdcardSignatureFromRecordsUseCase
import com.nunchuk.android.usecase.signer.GenerateColdCardHealthCheckMessageStringUseCase
import com.nunchuk.android.usecase.signer.GenerateMessageSigningQrUseCase
import com.nunchuk.android.usecase.signer.GenerateKruxMessageSigningUseCase
import com.nunchuk.android.usecase.signer.GeneratePassportMessageSigningUseCase
import com.nunchuk.android.usecase.signer.GetRemoteOrMasterSignerUseCase
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileOutputStream
import com.nunchuk.android.core.util.isPassportAirgap
import com.nunchuk.android.core.util.isKruxAirgap

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
    private val extractMessageSignatureUseCase: ExtractMessageSignatureUseCase,
    private val generatePassportMessageSigningUseCase: GeneratePassportMessageSigningUseCase,
    private val generateKruxMessageSigningUseCase: GenerateKruxMessageSigningUseCase,
    private val generateMessageSigningQrUseCase: GenerateMessageSigningQrUseCase,
    private val extractColdcardSignatureFromRecordsUseCase: ExtractColdcardSignatureFromRecordsUseCase,
    private val getRemoteOrMasterSignerUseCase: GetRemoteOrMasterSignerUseCase,
    private val getBitBoxSignMessagePathUseCase: GetBitBoxSignMessagePathUseCase,
    private val getTrezorSignMessageDeeplinkUseCase: GetTrezorSignMessageDeeplinkUseCase,
    private val parseTrezorSignMessageResponseUseCase: ParseTrezorSignMessageResponseUseCase,
    private val trezorCallbackHolder: TrezorCallbackHolder,
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

    private var lastHandledTrezorCallback: String = ""
    /** Set while a Trezor Suite sign-message request from this screen is outstanding. */
    private var awaitingTrezorSignature: Boolean = false

    init {
        // Trezor Suite answers over a deeplink that lands in TrezorCallbackHolder; only a
        // sign-message reply is ours (the add-key and sign-transaction screens filter theirs).
        viewModelScope.launch {
            trezorCallbackHolder.callbackUri.filterNotNull().collect { callbackUri ->
                if (handleTrezorCallback(callbackUri)) {
                    trezorCallbackHolder.clear(callbackUri)
                }
            }
        }
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

    /** The challenge as a Specter-format QR request for an air-gapped device (Jade). */
    suspend fun airgapSignMessageRequest(): String =
        generateMessageSigningQrUseCase(
            GenerateMessageSigningQrUseCase.Param(
                derivationPath = signer.derivationPath,
                message = message,
            )
        ).getOrElse { error ->
            Timber.e(error, "Failed to build the air-gap sign-message request")
            _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
            ""
        }

    /** The challenge for the device's own transport, as the [SingleSigner] the use cases take. */
    private suspend fun singleSigner(): SingleSigner? =
        getRemoteOrMasterSignerUseCase(
            GetRemoteOrMasterSignerUseCase.Data(
                id = signer.fingerPrint,
                derivationPath = signer.derivationPath,
            )
        ).onFailure { error ->
            Timber.e(error, "Failed to load signer for hardware signing")
            _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
        }.getOrNull()

    /**
     * BitBox signs in-app, but not at the signer's own path: the SDK resolves the key it can sign
     * with from the signer's descriptor (see GetBitBoxSignMessagePathUseCase). The sheet opens once
     * that path is known.
     */
    fun requestSignMessageByBitBox() {
        viewModelScope.launch {
            _state.update { it.copy(loadingType = LoadingType.Normal) }
            singleSigner()?.let { single ->
                getBitBoxSignMessagePathUseCase(GetBitBoxSignMessagePathUseCase.Param(single))
                    .onSuccess { path -> _state.update { it.copy(bitBoxSignMessagePath = path) } }
                    .onFailure { error ->
                        _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
                    }
            }
            _state.update { it.copy(loadingType = null) }
        }
    }

    fun dismissBitBoxSheet() = _state.update { it.copy(bitBoxSignMessagePath = null) }

    /** Trezor signs out of the app: build the Trezor Suite deeplink and let the screen confirm it. */
    fun requestSignMessageByTrezor() {
        viewModelScope.launch {
            _state.update { it.copy(loadingType = LoadingType.Normal) }
            singleSigner()?.let { single ->
                getTrezorSignMessageDeeplinkUseCase(
                    GetTrezorSignMessageDeeplinkUseCase.Param(signer = single, message = message)
                ).onSuccess { deeplink ->
                    awaitingTrezorSignature = true
                    _state.update { it.copy(trezorSuiteDeeplink = deeplink) }
                }.onFailure { error ->
                    _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
                }
            }
            _state.update { it.copy(loadingType = null) }
        }
    }

    fun dismissTrezorSuiteDialog() = _state.update { it.copy(trezorSuiteDeeplink = null) }

    /**
     * Same guards as the other Trezor callback handlers: not ours unless it is a sign-message
     * reply this screen asked for, handled once per URI, and only when Suite actually returned
     * something. The "asked for" guard is what keeps a two-key claim straight: a reply belongs
     * to the key whose screen requested it.
     */
    private fun handleTrezorCallback(callbackUri: String): Boolean {
        val callback = parseTrezorCallback(callbackUri)
            ?.takeIf { it.method == TrezorCallbackMethod.SIGN_MESSAGE && awaitingTrezorSignature }
            ?: return false
        if (lastHandledTrezorCallback == callback.rawUri) return true
        lastHandledTrezorCallback = callback.rawUri
        awaitingTrezorSignature = false
        if (callback.response.isBlank()) return true

        viewModelScope.launch {
            _state.update { it.copy(loadingType = LoadingType.Normal) }
            parseTrezorSignMessageResponseUseCase(
                ParseTrezorSignMessageResponseUseCase.Param(
                    response = callback.response,
                    message = callback.message.ifBlank { message },
                )
            ).onSuccess { signedMessage ->
                _state.update { it.copy(signedMessage = signedMessage) }
                signedMessage.signature.ifBlank { _event.emit(VerifyInheritanceMessageEvent.NoSignatureDetected) }
            }.onFailure { error ->
                _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
            }
            _state.update { it.copy(loadingType = null) }
        }
        return true
    }

    fun resetSignature() {
        _state.update { it.copy(signedMessage = null) }
    }

    /**
     * The message-signing request in the format the signer reads from a memory card, built once.
     * Each device has its own file format, all owned by libnunchuk.
     */
    suspend fun generateMessageFileIfNeeded(): String {
        val currentState = _state.value
        if (!currentState.messageFile.isNullOrEmpty()) return currentState.messageFile
        return buildMessageFile().onSuccess { messageFile ->
            _state.update { it.copy(messageFile = messageFile) }
        }.getOrElse { error ->
            _event.emit(VerifyInheritanceMessageEvent.ShowError(error.message.orUnknownError()))
            ""
        }
    }

    private suspend fun buildMessageFile(): Result<String> = when {
        signer.isPassportAirgap -> generatePassportMessageSigningUseCase(
            GeneratePassportMessageSigningUseCase.Param(
                derivationPath = signer.derivationPath,
                message = message,
            )
        )

        signer.isKruxAirgap -> generateKruxMessageSigningUseCase(
            GenerateKruxMessageSigningUseCase.Param(
                derivationPath = signer.derivationPath,
                message = message,
            )
        )

        else -> generateColdCardHealthCheckMessageStringUseCase(
            GenerateColdCardHealthCheckMessageStringUseCase.Param(
                derivationPath = signer.derivationPath,
                message = message,
                addressType = AddressType.LEGACY
            )
        )
    }

    /** Name the request file is saved or shared under; Passport lists `.txt` files from its card. */
    private val messageFileName: String
        get() = when {
            signer.isPassportAirgap -> "passport_message.txt"
            signer.isKruxAirgap -> "krux_message.txt"
            else -> "coldcard_message.txt"
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
            createShareFileUseCase(messageFileName).onSuccess { filePath ->
                exportTransaction(filePath, dataToSign)
            }.onFailure {
                _event.emit(VerifyInheritanceMessageEvent.ShowError(it.message.orUnknownError()))
                _state.update { it.copy(loadingType = null) }
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
                    fileName = messageFileName,
                    fileContent = dataToSign
                )
            )
            _state.update { it.copy(loadingType = null) }
            _event.emit(VerifyInheritanceMessageEvent.SaveLocalFile(result.isSuccess))
        }
    }

    fun handleExportTransactionToMk4(ndef: Ndef) {
        viewModelScope.launch {
            generateMessageFileIfNeeded()
            val messageFile = _state.value.messageFile
            if (!messageFile.isNullOrEmpty()) {
                exportToMk4(messageFile, ndef)
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

                val signature = extractMessageSignatureUseCase(fileContent).getOrThrow()
                _state.update {
                    it.copy(
                        signedMessage = SignedMessage(
                            signature = signature,
                        ),
                    )
                }
            } catch (e: Exception) {
                if (e.nativeErrorCode() == NativeErrorCode.INVALID_SIGNED_MESSAGE) {
                    _event.emit(VerifyInheritanceMessageEvent.ShowError("Invalid signature. Please try again."))
                } else {
                    _event.emit(VerifyInheritanceMessageEvent.ShowError(e.message.orUnknownError()))
                }
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
                    if (e.nativeErrorCode() == NativeErrorCode.INVALID_SIGNED_MESSAGE) {
                        _event.emit(VerifyInheritanceMessageEvent.ShowError("Invalid signature. Please try again."))
                    } else {
                        _event.emit(VerifyInheritanceMessageEvent.ShowError(e.message.orUnknownError()))
                    }
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
    val messageFile: String? = null,
    /** Path the BitBox sheet signs at; the sheet is shown while this is set. */
    val bitBoxSignMessagePath: String? = null,
    /** Trezor Suite deeplink awaiting the user's confirmation; the dialog is shown while set. */
    val trezorSuiteDeeplink: String? = null,
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
