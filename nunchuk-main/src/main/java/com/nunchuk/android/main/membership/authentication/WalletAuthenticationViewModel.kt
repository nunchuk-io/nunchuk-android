/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.main.membership.authentication

import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GenerateColdCardHealthCheckMessageUseCase
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.domain.HealthCheckColdCardUseCase
import com.nunchuk.android.core.domain.coldcard.ExportRawPsbtToMk4UseCase
import com.nunchuk.android.core.domain.membership.CheckSignMessageTapsignerUseCase
import com.nunchuk.android.core.domain.membership.CheckSignMessageUseCase
import com.nunchuk.android.core.domain.membership.GetHealthCheckMessageUseCase
import com.nunchuk.android.core.domain.membership.GetSignatureFromColdCardPsbt
import com.nunchuk.android.core.nfc.NfcScanInfo
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.membership.GetDummyTransactionSignatureUseCase
import com.nunchuk.android.usecase.membership.GetDummyTxFromPsbt
import com.nunchuk.android.usecase.membership.GetTxToSignMessage
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class WalletAuthenticationViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val checkSignMessageUseCase: CheckSignMessageUseCase,
    private val checkSignMessageTapsignerUseCase: CheckSignMessageTapsignerUseCase,
    private val getHealthCheckMessageUseCase: GetHealthCheckMessageUseCase,
    private val healthCheckColdCardUseCase: HealthCheckColdCardUseCase,
    private val generateColdCardHealthCheckMessageUseCase: GenerateColdCardHealthCheckMessageUseCase,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
    private val getDummyTxFromPsbt: GetDummyTxFromPsbt,
    private val getTxToSignMessage: GetTxToSignMessage,
    private val exportRawPsbtToMk4UseCase: ExportRawPsbtToMk4UseCase,
    private val getSignatureFromColdCardPsbt: GetSignatureFromColdCardPsbt,
    private val getDummyTransactionSignatureUseCase: GetDummyTransactionSignatureUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = WalletAuthenticationActivityArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<WalletAuthenticationEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(WalletAuthenticationState())
    val state = _state.asStateFlow()

    private val dataToSign = MutableStateFlow("")

    init {
        savedStateHandle.get<SingleSigner>(EXTRA_CURRENT_INTERACT_SIGNER)?.let { signer ->
            _state.update { it.copy(interactSingleSigner = signer) }
        }
        getWalletDetails()
        viewModelScope.launch {
            if (args.type == WalletAuthenticationActivity.SIGN_DUMMY_TX) {
                val txToSignResult =
                    getTxToSignMessage(GetTxToSignMessage.Param(args.walletId, args.userData))
                if (txToSignResult.isFailure) {
                    _event.emit(WalletAuthenticationEvent.ShowError(txToSignResult.exceptionOrNull()?.message.orUnknownError()))
                    return@launch
                }
                dataToSign.value = txToSignResult.getOrNull().orEmpty()
                val result = getDummyTxFromPsbt(
                    GetDummyTxFromPsbt.Param(
                        walletId = args.walletId,
                        psbt = dataToSign.value
                    )
                )
                if (result.isSuccess) {
                    _state.update { it.copy(transaction = result.getOrThrow()) }
                } else {
                    _event.emit(WalletAuthenticationEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
                }
            } else {
                dataToSign.value = getHealthCheckMessageUseCase(args.userData).getOrThrow()
            }
        }
    }

    fun onSignerSelect(signerModel: SignerModel) = viewModelScope.launch {
        _state.update { it.copy(interactSingleSigner = null) }
        val singleSigner =
            _state.value.singleSigners.firstOrNull { it.masterSignerId == signerModel.id && it.derivationPath == signerModel.derivationPath }
                ?: return@launch
        savedStateHandle[EXTRA_CURRENT_INTERACT_SIGNER] = singleSigner
        _state.update { it.copy(interactSingleSigner = singleSigner) }
        when (signerModel.type) {
            SignerType.NFC -> _event.emit(WalletAuthenticationEvent.ScanTapSigner)
            SignerType.COLDCARD_NFC -> _event.emit(WalletAuthenticationEvent.ScanColdCard)
            SignerType.SOFTWARE,
            SignerType.HARDWARE -> handleSignCheckSoftware(singleSigner)
            SignerType.AIRGAP -> _event.emit(WalletAuthenticationEvent.ShowAirgapOption)
            else -> {}
        }
    }

    fun handleExportTransactionToMk4(ndef: Ndef) {
        viewModelScope.launch {
            val transaction = _state.value.transaction ?: return@launch
            _event.emit(WalletAuthenticationEvent.NfcLoading(isLoading = true, isColdCard = true))
            val result =
                exportRawPsbtToMk4UseCase(ExportRawPsbtToMk4UseCase.Data(transaction.psbt, ndef))
            _event.emit(WalletAuthenticationEvent.NfcLoading(isLoading = false, isColdCard = true))
            if (result.isSuccess) {
                _event.emit(WalletAuthenticationEvent.ExportTransactionToColdcardSuccess)
            } else {
                _event.emit(WalletAuthenticationEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun generateColdcardHealthMessages(ndef: Ndef?, derivationPath: String) {
        ndef ?: return
        viewModelScope.launch {
            _event.emit(WalletAuthenticationEvent.NfcLoading(isLoading = true, isColdCard = true))
            val result = generateColdCardHealthCheckMessageUseCase(
                GenerateColdCardHealthCheckMessageUseCase.Data(
                    derivationPath = derivationPath,
                    ndef = ndef
                )
            )
            _event.emit(WalletAuthenticationEvent.NfcLoading(false))
            if (result.isSuccess) {
                _event.emit(WalletAuthenticationEvent.GenerateColdcardHealthMessagesSuccess)
            } else {
                _event.emit(WalletAuthenticationEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun healthCheckColdCard(signer: SingleSigner, records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(WalletAuthenticationEvent.NfcLoading(isLoading = true, isColdCard = true))
            val result =
                healthCheckColdCardUseCase(HealthCheckColdCardUseCase.Param(signer, records))
            _event.emit(WalletAuthenticationEvent.NfcLoading(false))
            handleSignatureResult(result = result.map { it.signature }, singleSigner = signer)
        }
    }

    fun generateSignatureFromColdCardPsbt(signer: SingleSigner, records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(WalletAuthenticationEvent.NfcLoading(isLoading = true, isColdCard = true))
            val result =
                getSignatureFromColdCardPsbt(GetSignatureFromColdCardPsbt.Data(signer, records))
            _event.emit(WalletAuthenticationEvent.NfcLoading(false))
            handleSignatureResult(result = result, singleSigner = signer)
        }
    }

    fun handleTapSignerSignCheckMessage(
        singleSigner: SingleSigner,
        ncfScanInfo: NfcScanInfo?,
        cvc: String
    ) = viewModelScope.launch {
        _event.emit(WalletAuthenticationEvent.NfcLoading(isLoading = true, isColdCard = false))
        val result = checkSignMessageTapsignerUseCase(
            CheckSignMessageTapsignerUseCase.Param(
                signer = singleSigner,
                isoDep = IsoDep.get(ncfScanInfo?.tag),
                cvc = cvc,
                messageToSign = dataToSign.value
            )
        )
        _event.emit(WalletAuthenticationEvent.Loading(false))
        handleSignatureResult(result, singleSigner)
    }

    fun handleImportAirgapTransaction(transaction: Transaction) {
        viewModelScope.launch {
            if (transaction.txId != _state.value.transaction?.txId) {
                val signatures = _state.value.signatures
                _state.value.singleSigners.filter {
                    it.type == SignerType.AIRGAP
                            && transaction.signers[it.masterSignerId] == true
                            && signatures.contains(it.masterSignerId).not()
                }.forEach {
                    handleSignatureResult(
                        getDummyTransactionSignatureUseCase(
                            GetDummyTransactionSignatureUseCase.Param(
                                it,
                                transaction.psbt
                            )
                        ),
                        it
                    )
                }
            } else {
                _event.emit(WalletAuthenticationEvent.ShowError("You import invalid transaction"))
            }
        }
    }

    private fun handleSignCheckSoftware(singleSigner: SingleSigner) {
        viewModelScope.launch {
            val result = checkSignMessageUseCase(
                CheckSignMessageUseCase.Param(
                    signer = singleSigner,
                    messageToSign = dataToSign.value,
                )
            )
            handleSignatureResult(result, singleSigner)
        }
    }

    suspend fun handleSignatureResult(
        result: Result<String>,
        singleSigner: SingleSigner
    ) {
        if (result.isSuccess) {
            val signatures = _state.value.signatures.toMutableMap()
            signatures[singleSigner.masterFingerprint] = result.getOrThrow()
            if (signatures.size == args.requiredSignatures) {
                _event.emit(WalletAuthenticationEvent.WalletAuthenticationSuccess(signatures))
            } else {
                _state.update {
                    it.copy(signatures = signatures)
                }
            }
        } else {
            _event.emit(WalletAuthenticationEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun getInteractSingleSigner() = _state.value.interactSingleSigner

    fun getDataToSign() = dataToSign.value

    fun getWalletId() = args.walletId

    private fun getWalletDetails() {
        viewModelScope.launch {
            getWalletUseCase.execute(args.walletId)
                .onStart { _event.emit(WalletAuthenticationEvent.Loading(true)) }
                .flowOn(Dispatchers.IO)
                .onException { _event.emit(WalletAuthenticationEvent.ProcessFailure(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { walletExtended ->
                    _event.emit(WalletAuthenticationEvent.Loading(false))
                    val signerModels =
                        walletExtended.wallet.signers.filter {
                            isValidSigner(it.type, args.type)
                        }.map {
                            if (it.type == SignerType.NFC) it.toModel()
                                .copy(cardId = getTapSignerStatusByIdUseCase(it.masterSignerId).getOrThrow().ident.orEmpty()) else it.toModel()
                        }
                    _state.update {
                        it.copy(
                            walletSigner = signerModels,
                            singleSigners = walletExtended.wallet.signers
                        )
                    }
                }
        }
    }

    private fun isValidSigner(type: SignerType, authenticationType: String): Boolean {
        if (authenticationType == WalletAuthenticationActivity.SIGN_DUMMY_TX && type == SignerType.AIRGAP) return true
        return type == SignerType.SOFTWARE
                || type == SignerType.HARDWARE
                || type == SignerType.NFC
                || type == SignerType.COLDCARD_NFC
    }

    companion object {
        private const val EXTRA_CURRENT_INTERACT_SIGNER = "EXTRA_CURRENT_INTERACT_SIGNER"
    }
}