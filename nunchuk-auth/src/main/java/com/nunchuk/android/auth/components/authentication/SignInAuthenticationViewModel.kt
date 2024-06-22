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

package com.nunchuk.android.auth.components.authentication

import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.auth.domain.SignInViaDigitalSignatureUseCase
import com.nunchuk.android.core.domain.coldcard.ExportRawPsbtToMk4UseCase
import com.nunchuk.android.core.domain.membership.CheckSignMessageTapsignerSignInUseCase
import com.nunchuk.android.core.domain.membership.GetSignatureFromColdCardPsbt
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.nfc.NfcScanInfo
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.byzantine.SignInDummyTransactionUpdate
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.usecase.GetSignInDummyTransactionUseCase
import com.nunchuk.android.usecase.byzantine.UpdateDummyTransactionSignInUseCase
import com.nunchuk.android.usecase.membership.GetDummyTransactionSignatureUseCase
import com.nunchuk.android.usecase.membership.GetSignInDummyTxFromPsbt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SignInAuthenticationViewModel @Inject constructor(
    private val checkSignMessageTapsignerSignInUseCase: CheckSignMessageTapsignerSignInUseCase,
    private val getSignInDummyTxFromPsbt: GetSignInDummyTxFromPsbt,
    private val exportRawPsbtToMk4UseCase: ExportRawPsbtToMk4UseCase,
    private val getSignatureFromColdCardPsbt: GetSignatureFromColdCardPsbt,
    private val getDummyTransactionSignatureUseCase: GetDummyTransactionSignatureUseCase,
    private val getSignInDummyTransactionUseCase: GetSignInDummyTransactionUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val updateDummyTransactionSignInUseCase: UpdateDummyTransactionSignInUseCase,
    private val signInViaDigitalSignatureUseCase: SignInViaDigitalSignatureUseCase,
    private val signInModeHolder: SignInModeHolder,
) : ViewModel() {

    private val args = SignInAuthenticationActivityArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<SignInAuthenticationEvent>()
    val event = _event.asSharedFlow()

    private val _state =
        MutableStateFlow(SignInAuthenticationState(pendingSignature = args.requiredSignatures))
    val state = _state.asStateFlow()

    private val dataToSign = MutableStateFlow("")

    init {
        savedStateHandle.get<SignerModel>(EXTRA_CURRENT_INTERACT_SIGNER)?.let { signer ->
            _state.update { it.copy(interactSignerModel = signer) }
        }
        viewModelScope.launch {
            _event.emit(SignInAuthenticationEvent.Loading(true))
            if (!args.dummyTransactionId.isNullOrEmpty()) {
                getSignInDummyTransactionUseCase(
                    GetSignInDummyTransactionUseCase.Param(args.signInData.orEmpty())
                ).onSuccess { signInDummyTransaction ->
                    dataToSign.value = signInDummyTransaction.psbt
                    _state.update {
                        it.copy(
                            pendingSignature = signInDummyTransaction.pendingSignature,
                            walletSigner = signInDummyTransaction.signerServers.map { it.toModel(0) },
                            signatures = signInDummyTransaction.signatures.associate { it.xfp to it.signature },
                        )
                    }
                    loadPsbt(signInDummyTransaction.psbt)
                }.onFailure {
                    _event.emit(SignInAuthenticationEvent.ShowError(it.message.orUnknownError()))
                    return@launch
                }
            }
            _event.emit(SignInAuthenticationEvent.Loading(false))
        }
    }

    private suspend fun loadPsbt(psbt: String) {
        val result =
            getSignInDummyTxFromPsbt(
                GetSignInDummyTxFromPsbt.Param(
                    psbt = psbt,
                )
            )
        if (result.isSuccess) {
            // hard code isReceive false I have no idea why first time it become true from libnunchuk
            _state.update {
                it.copy(
                    transaction = result.getOrThrow()
                        .copy(isReceive = false, status = TransactionStatus.PENDING_SIGNATURES)
                )
            }
        } else {
            _event.emit(SignInAuthenticationEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun onSignerSelect(signerModel: SignerModel) = viewModelScope.launch {
        _state.update { it.copy(interactSignerModel = null) }
        savedStateHandle[EXTRA_CURRENT_INTERACT_SIGNER] = signerModel
        _state.update { it.copy(interactSignerModel = signerModel) }
        when {
            signerModel.type == SignerType.NFC -> _event.emit(SignInAuthenticationEvent.ScanTapSigner)
            signerModel.type == SignerType.COLDCARD_NFC
                    || (signerModel.type == SignerType.HARDWARE
                    && signerModel.tags.contains(SignerTag.COLDCARD)) -> {
                _event.emit(SignInAuthenticationEvent.ScanColdCard)
            }

            signerModel.type == SignerType.HARDWARE -> _event.emit(SignInAuthenticationEvent.CanNotSignHardwareKey)
            signerModel.type == SignerType.AIRGAP -> _event.emit(SignInAuthenticationEvent.ShowAirgapOption)
            else -> {}
        }
    }

    fun handleExportTransactionToMk4(ndef: Ndef) {
        viewModelScope.launch {
            val transaction = _state.value.transaction ?: return@launch
            _event.emit(SignInAuthenticationEvent.NfcLoading(isLoading = true, isColdCard = true))
            val result =
                exportRawPsbtToMk4UseCase(ExportRawPsbtToMk4UseCase.Data(transaction.psbt, ndef))
            _event.emit(SignInAuthenticationEvent.NfcLoading(isLoading = false, isColdCard = true))
            if (result.isSuccess) {
                _event.emit(SignInAuthenticationEvent.ExportTransactionToColdcardSuccess)
            } else {
                _event.emit(SignInAuthenticationEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun generateSignatureFromColdCardPsbt(signer: SignerModel, records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(SignInAuthenticationEvent.NfcLoading(isLoading = true, isColdCard = true))
            val result = getSignatureFromColdCardPsbt(
                GetSignatureFromColdCardPsbt.Data(
                    signer.toSingleSigner(),
                    records
                )
            )
            handleSignatureResult(result = result, signerModel = signer)
            _event.emit(SignInAuthenticationEvent.NfcLoading(false))
        }
    }

    fun handleTapSignerSignCheckMessage(
        signerModel: SignerModel,
        ncfScanInfo: NfcScanInfo?,
        cvc: String,
    ) = viewModelScope.launch {
        _event.emit(SignInAuthenticationEvent.NfcLoading(isLoading = true, isColdCard = false))
        val result = checkSignMessageTapsignerSignInUseCase(
            CheckSignMessageTapsignerSignInUseCase.Param(
                isoDep = IsoDep.get(ncfScanInfo?.tag),
                cvc = cvc,
                messageToSign = dataToSign.value,
                signer = signerModel.toSingleSigner()
            )
        )
        _event.emit(SignInAuthenticationEvent.Loading(false))
        handleSignatureResult(result, signerModel)
    }

    fun handleImportAirgapTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val signatures = _state.value.signatures
            val validSignatures = _state.value.walletSigner.filter {
                transaction.signers[it.id] == true
                        && signatures.contains(it.id).not()
            }
            if (validSignatures.isEmpty()) {
                getInteractSingleSigner()?.let {
                    _event.emit(SignInAuthenticationEvent.SignFailed(it))
                }
                return@launch
            }
            validSignatures.forEach {
                handleSignatureResult(
                    getDummyTransactionSignatureUseCase(
                        GetDummyTransactionSignatureUseCase.Param(
                            it.toSingleSigner(),
                            transaction.psbt
                        )
                    ),
                    it
                )
            }
        }
    }

    private suspend fun handleSignatureResult(
        result: Result<String>,
        signerModel: SignerModel,
    ) {
        if (result.isSuccess) {
            val signatures = _state.value.signatures.toMutableMap()
            val signature = result.getOrThrow()
            if (signature.isEmpty()) {
                _event.emit(SignInAuthenticationEvent.CanNotSignDummyTx)
                return
            }
            signatures[signerModel.id] = signature
            if (uploadSignature(signerModel.id, signature, signatures)) {
                val status = _state.value.transactionStatus
                if (status != TransactionStatus.CONFIRMED) {
                    _event.emit(SignInAuthenticationEvent.UploadSignatureSuccess(status))
                }
            }
        } else {
            _event.emit(SignInAuthenticationEvent.SignFailed(signerModel))
            _event.emit(SignInAuthenticationEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    private suspend fun uploadSignature(
        masterFingerprint: String,
        signature: String,
        signatures: MutableMap<String, String>,
    ): Boolean {
        return updateDummyTransactionSignInUseCase(
            UpdateDummyTransactionSignInUseCase.Param(
                signatures = mapOf(masterFingerprint to signature),
                transactionId = args.dummyTransactionId.orEmpty()
            )
        ).onFailure {
            _event.emit(SignInAuthenticationEvent.ShowError(it.message.orUnknownError()))
        }.onSuccess { updateInfo ->
            _state.update {
                it.copy(
                    signatures = signatures,
                    pendingSignature = updateInfo.pendingSignatures,
                    transactionStatus = updateInfo.status
                )
            }
            if (updateInfo.status == TransactionStatus.CONFIRMED) {
                signIn(updateInfo)
            }
        }.isSuccess
    }

    private fun signIn(dummyTransactionUpdate: SignInDummyTransactionUpdate) {
        viewModelScope.launch {
            _event.emit(SignInAuthenticationEvent.Loading(true))
            val result = signInViaDigitalSignatureUseCase(
                SignInViaDigitalSignatureUseCase.Param(
                    staySignedIn = true,
                    tokenId = dummyTransactionUpdate.tokenId,
                    deviceId = dummyTransactionUpdate.deviceId
                )
            )
            _event.emit(SignInAuthenticationEvent.Loading(false))
            if (result.isSuccess) {
                signInModeHolder.setCurrentMode(SignInMode.EMAIL)
                _event.emit(
                    SignInAuthenticationEvent.SignInSuccess(
                        token = dummyTransactionUpdate.tokenId,
                        deviceId = dummyTransactionUpdate.deviceId
                    )
                )
            } else {
                _event.emit(SignInAuthenticationEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun getInteractSingleSigner() = _state.value.interactSignerModel

    fun getDataToSign() = dataToSign.value

    companion object {
        private const val EXTRA_CURRENT_INTERACT_SIGNER = "EXTRA_CURRENT_INTERACT_SIGNER"
    }
}