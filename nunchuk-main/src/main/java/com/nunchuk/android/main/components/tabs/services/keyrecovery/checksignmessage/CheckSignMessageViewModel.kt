package com.nunchuk.android.main.components.tabs.services.keyrecovery.checksignmessage

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.CheckSignMessageTapsignerUseCase
import com.nunchuk.android.core.domain.membership.CheckSignMessageUseCase
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
    savedStateHandle: SavedStateHandle
) :
    ViewModel() {

    private val args = CheckSignMessageFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<CheckSignMessageEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CheckSignMessageState())
    val state = _state.asStateFlow()

    private val heathcheckableType =
        setOf(SignerType.NFC, SignerType.SOFTWARE, SignerType.COLDCARD_NFC)

    init {
        getWalletDetails()
    }

    fun onSignerSelect(signerModel: SignerModel) = viewModelScope.launch {
        _state.update { it.copy(interactSingleSigner = null) }
        val singleSigner =
            _state.value.singleSigners.firstOrNull { it.masterSignerId == signerModel.id }
                ?: return@launch
        if (signerModel.type == SignerType.NFC) {
            _state.update { it.copy(interactSingleSigner = singleSigner) }
            _event.emit(CheckSignMessageEvent.OpenScanDataTapsigner)
            return@launch
        }
        handleSignCheckMessage(singleSigner)
    }

    fun handleSignCheckMessage(
        singleSigner: SingleSigner,
        ncfScanInfo: NfcScanInfo? = null,
        cvc: String? = null
    ) = viewModelScope.launch {
        _event.emit(CheckSignMessageEvent.Loading(true))
        val result = if (singleSigner.type == SignerType.NFC) {
            checkSignMessageTapsignerUseCase(
                CheckSignMessageTapsignerUseCase.Param(
                    signer = singleSigner,
                    userData = args.userData,
                    isoDep = IsoDep.get(ncfScanInfo?.tag),
                    cvc = cvc!!
                )
            )
        } else {
            checkSignMessageUseCase(
                CheckSignMessageUseCase.Param(
                    signer = singleSigner,
                    userData = args.userData,
                )
            )
        }
        _event.emit(CheckSignMessageEvent.Loading(false))
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
                        walletExtended.wallet.signers.filter { it.type in heathcheckableType }
                            .map { it.toModel() }
                    _state.update {
                        it.copy(
                            signerModels = signerModels,
                            singleSigners = walletExtended.wallet.signers
                        )
                    }
                    _event.emit(CheckSignMessageEvent.GetSignersSuccess(signerModels))
                }
        }
    }
}