package com.nunchuk.android.main.components.tabs.services.keyrecovery.checksignmessage

import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GenerateColdCardHealthCheckMessageUseCase
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.domain.HealthCheckColdCardUseCase
import com.nunchuk.android.core.domain.membership.CheckSignMessageTapsignerUseCase
import com.nunchuk.android.core.domain.membership.CheckSignMessageUseCase
import com.nunchuk.android.core.domain.membership.GetHealthCheckMessageUseCase
import com.nunchuk.android.core.nfc.NfcScanInfo
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CheckSignMessageViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val checkSignMessageUseCase: CheckSignMessageUseCase,
    private val checkSignMessageTapsignerUseCase: CheckSignMessageTapsignerUseCase,
    private val getHealthCheckMessageUseCase: GetHealthCheckMessageUseCase,
    private val healthCheckColdCardUseCase: HealthCheckColdCardUseCase,
    private val generateColdCardHealthCheckMessageUseCase: GenerateColdCardHealthCheckMessageUseCase,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
    savedStateHandle: SavedStateHandle
) :
    ViewModel() {

    private val args = CheckSignMessageFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<CheckSignMessageEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CheckSignMessageState())
    val state = _state.asStateFlow()

    private val messageToSign = MutableStateFlow("")

    init {
        getWalletDetails()
        viewModelScope.launch {
            messageToSign.value = getHealthCheckMessageUseCase(args.userData).getOrThrow()
        }
    }

    fun onSignerSelect(signerModel: SignerModel) = viewModelScope.launch {
        _state.update { it.copy(interactSingleSigner = null) }
        val singleSigner =
            _state.value.singleSigners.firstOrNull { it.masterSignerId == signerModel.id && it.derivationPath == signerModel.derivationPath }
                ?: return@launch
        _state.update { it.copy(interactSingleSigner = singleSigner) }
        when (signerModel.type) {
            SignerType.NFC -> _event.emit(CheckSignMessageEvent.ScanTapSigner)
            SignerType.COLDCARD_NFC -> _event.emit(CheckSignMessageEvent.ScanColdCard)
            SignerType.SOFTWARE -> handleSignCheckSoftware(singleSigner)
            else -> {}
        }
    }

    fun generateColdcardHealthMessages(ndef: Ndef?, derivationPath: String) {
        ndef ?: return
        viewModelScope.launch {
            _event.emit(CheckSignMessageEvent.NfcLoading(isLoading = true, isColdCard = true))
            val result = generateColdCardHealthCheckMessageUseCase(
                GenerateColdCardHealthCheckMessageUseCase.Data(
                    derivationPath = derivationPath,
                    ndef = ndef
                )
            )
            _event.emit(CheckSignMessageEvent.NfcLoading(false))
            if (result.isSuccess) {
                _event.emit(CheckSignMessageEvent.GenerateColdcardHealthMessagesSuccess)
            } else {
                _event.emit(CheckSignMessageEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun healthCheckColdCard(signer: SingleSigner, records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(CheckSignMessageEvent.NfcLoading(isLoading = true, isColdCard = true))
            val result =
                healthCheckColdCardUseCase(HealthCheckColdCardUseCase.Param(signer, records))
            _event.emit(CheckSignMessageEvent.NfcLoading(false))
            handleSignatureResult(result = result.map { it.signature }, singleSigner = signer)
        }
    }

    fun handleTapSignerSignCheckMessage(
        singleSigner: SingleSigner,
        ncfScanInfo: NfcScanInfo?,
        cvc: String
    ) = viewModelScope.launch {
        _event.emit(CheckSignMessageEvent.NfcLoading(isLoading = true, isColdCard = false))
        val result = checkSignMessageTapsignerUseCase(
            CheckSignMessageTapsignerUseCase.Param(
                signer = singleSigner,
                userData = args.userData,
                isoDep = IsoDep.get(ncfScanInfo?.tag),
                cvc = cvc,
                messageToSign = messageToSign.value
            )
        )
        _event.emit(CheckSignMessageEvent.Loading(false))
        handleSignatureResult(result, singleSigner)
    }

    private fun handleSignCheckSoftware(singleSigner: SingleSigner) {
        viewModelScope.launch {
            val result = checkSignMessageUseCase(
                CheckSignMessageUseCase.Param(
                    signer = singleSigner,
                    userData = args.userData,
                )
            )
            handleSignatureResult(result, singleSigner)
        }
    }

    private suspend fun handleSignatureResult(
        result: Result<String>,
        singleSigner: SingleSigner
    ) {
        if (result.isSuccess) {
            val signatures = _state.value.signatures
            signatures[singleSigner.masterFingerprint] = result.getOrThrow()
            if (signatures.size == args.requiredSignatures) {
                _event.emit(CheckSignMessageEvent.CheckSignMessageSuccess(signatures))
            } else {
                _state.update {
                    it.copy(signatures = signatures)
                }
            }
        } else {
            _event.emit(CheckSignMessageEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun getInteractSingleSigner() = _state.value.interactSingleSigner

    private fun getWalletDetails() {
        viewModelScope.launch {
            getWalletUseCase.execute(args.walletId)
                .onStart { _event.emit(CheckSignMessageEvent.Loading(true)) }
                .flowOn(Dispatchers.IO)
                .onException { _event.emit(CheckSignMessageEvent.ProcessFailure(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { walletExtended ->
                    _event.emit(CheckSignMessageEvent.Loading(false))
                    val signerModels =
                        walletExtended.wallet.signers.filter {
                            it.type == SignerType.SOFTWARE
                                    || it.type == SignerType.HARDWARE
                                    || it.type == SignerType.NFC
                                    || it.type == SignerType.COLDCARD_NFC
                        }.map {
                            if (it.type == SignerType.NFC) it.toModel()
                                .copy(cardId = getTapSignerStatusByIdUseCase(it.masterSignerId).getOrThrow().ident.orEmpty()) else it.toModel()
                        }
                    _state.update {
                        it.copy(
                            signerModels = signerModels,
                            singleSigners = walletExtended.wallet.signers
                        )
                    }
                }
        }
    }
}