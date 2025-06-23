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

package com.nunchuk.android.main.membership.authentication

import android.app.Application
import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GenerateColdCardHealthCheckMessageUseCase
import com.nunchuk.android.core.domain.HealthCheckColdCardUseCase
import com.nunchuk.android.core.domain.byzantine.ParsePendingHealthCheckPayloadUseCase
import com.nunchuk.android.core.domain.coldcard.ExportRawPsbtToMk4UseCase
import com.nunchuk.android.core.domain.membership.CheckSignMessageTapsignerUseCase
import com.nunchuk.android.core.domain.membership.CheckSignMessageUseCase
import com.nunchuk.android.core.domain.membership.GetSignatureFromColdCardPsbt
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.nfc.NfcScanInfo
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.core.util.isNoInternetException
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.R
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.byzantine.DummyTransactionUpdate
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.SendSignerPassphrase
import com.nunchuk.android.usecase.byzantine.DeleteGroupDummyTransactionUseCase
import com.nunchuk.android.usecase.byzantine.FinalizeDummyTransactionUseCase
import com.nunchuk.android.usecase.byzantine.GetDummyTxRequestTokenUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupDummyTransactionUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletUseCase
import com.nunchuk.android.usecase.byzantine.UpdateGroupDummyTransactionUseCase
import com.nunchuk.android.usecase.membership.GetDummyTransactionSignatureUseCase
import com.nunchuk.android.usecase.membership.GetDummyTxFromPsbt
import com.nunchuk.android.usecase.membership.GetTxToSignMessage
import com.nunchuk.android.usecase.network.NetworkStatusFlowUseCase
import com.nunchuk.android.usecase.signer.ClearSignerPassphraseUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class WalletAuthenticationViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val checkSignMessageUseCase: CheckSignMessageUseCase,
    private val checkSignMessageTapsignerUseCase: CheckSignMessageTapsignerUseCase,
    private val parsePendingHealthCheckPayloadUseCase: ParsePendingHealthCheckPayloadUseCase,
    private val healthCheckColdCardUseCase: HealthCheckColdCardUseCase,
    private val generateColdCardHealthCheckMessageUseCase: GenerateColdCardHealthCheckMessageUseCase,
    private val cardIdManager: CardIdManager,
    private val getDummyTxFromPsbt: GetDummyTxFromPsbt,
    private val getTxToSignMessage: GetTxToSignMessage,
    private val exportRawPsbtToMk4UseCase: ExportRawPsbtToMk4UseCase,
    private val getSignatureFromColdCardPsbt: GetSignatureFromColdCardPsbt,
    private val getDummyTransactionSignatureUseCase: GetDummyTransactionSignatureUseCase,
    private val getGroupDummyTransactionUseCase: GetGroupDummyTransactionUseCase,
    private val updateGroupDummyTransactionUseCase: UpdateGroupDummyTransactionUseCase,
    private val finalizeDummyTransactionUseCase: FinalizeDummyTransactionUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val deleteGroupDummyTransactionUseCase: DeleteGroupDummyTransactionUseCase,
    private val getDummyTxRequestTokenUseCase: GetDummyTxRequestTokenUseCase,
    private val networkStatusFlowUseCase: NetworkStatusFlowUseCase,
    private val application: Application,
    private val syncGroupWalletUseCase: SyncGroupWalletUseCase,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase,
    private val clearSignerPassphraseUseCase: ClearSignerPassphraseUseCase,
) : ViewModel() {

    private val args = WalletAuthenticationActivityArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<WalletAuthenticationEvent>()
    val event = _event.asSharedFlow()

    private val _state =
        MutableStateFlow(WalletAuthenticationState(pendingSignature = args.requiredSignatures))
    val state = _state.asStateFlow()

    private val dataToSign = MutableStateFlow("")

    init {
        savedStateHandle.get<SingleSigner>(EXTRA_CURRENT_INTERACT_SIGNER)?.let { signer ->
            _state.update { it.copy(interactSingleSigner = signer) }
        }
        viewModelScope.launch {
            _event.emit(WalletAuthenticationEvent.Loading(true))
            if (!args.dummyTransactionId.isNullOrEmpty()) {
                getGroupDummyTransactionUseCase(
                    GetGroupDummyTransactionUseCase.Param(
                        groupId = args.groupId.orEmpty(),
                        walletId = args.walletId,
                        transactionId = args.dummyTransactionId.orEmpty()
                    )
                ).onSuccess { dummyTransaction ->
                    dataToSign.value = dummyTransaction.psbt
                    _state.update {
                        it.copy(
                            pendingSignature = dummyTransaction.pendingSignature,
                            dummyTransactionType = dummyTransaction.dummyTransactionType,
                            isDraft = dummyTransaction.isDraft
                        )
                    }
                    if (dummyTransaction.dummyTransactionType == DummyTransactionType.HEALTH_CHECK_PENDING
                        || dummyTransaction.dummyTransactionType == DummyTransactionType.HEALTH_CHECK_REQUEST
                    ) {
                        parsePendingHealthCheckPayloadUseCase(dummyTransaction)
                            .onSuccess { payload ->
                                _state.update {
                                    it.copy(enabledSigners = setOf(payload.keyXfp.orEmpty()))
                                }
                            }
                    }
                    loadPsbt()
                }.onFailure {
                    _event.emit(WalletAuthenticationEvent.ShowError(it.message.orUnknownError()))
                    return@launch
                }
            } else if (args.type == VerificationType.SIGN_DUMMY_TX) {
                getTxToSignMessage(GetTxToSignMessage.Param(args.walletId, args.userData))
                    .onSuccess { psbt ->
                        dataToSign.value = psbt
                        loadPsbt()
                    }.onFailure {
                        _event.emit(WalletAuthenticationEvent.ShowError(it.message.orUnknownError()))
                    }
            }
            _event.emit(WalletAuthenticationEvent.Loading(false))
        }
        if (!args.dummyTransactionId.isNullOrEmpty()) {
            viewModelScope.launch {
                networkStatusFlowUseCase(Unit).map { it.getOrThrow() }.collect { isConnected ->
                    if (isConnected) {
                        uploadSignaturesFromLocalIfNeeded()
                    }
                }
            }
        }
    }

    fun uploadSignaturesFromLocalIfNeeded(isShowMessage: Boolean = false) {
        if (!args.dummyTransactionId.isNullOrEmpty()) {
            viewModelScope.launch {
                getDummyTxRequestTokenUseCase(
                    GetDummyTxRequestTokenUseCase.Param(
                        walletId = args.walletId,
                        transactionId = args.dummyTransactionId.orEmpty()
                    )
                ).onSuccess { tokens ->
                    val signatures = _state.value.signatures.toMutableMap()
                    tokens.filter { it.value }.forEach {
                        val (xfp, token) = it.key.split(".")
                        signatures[xfp] = token
                    }
                    tokens.filter { it.value.not() }.forEach {
                        val (xfp, token) = it.key.split(".")
                        uploadSignature(xfp, token, signatures).onSuccess {
                            signatures[xfp] = token
                        }.onFailure { e ->
                            if (isShowMessage) {
                                if (e.isNoInternetException) {
                                    _event.emit(WalletAuthenticationEvent.NoInternetConnectionForceSync)
                                } else {
                                    _event.emit(WalletAuthenticationEvent.ShowError(e.message.orUnknownError()))
                                }
                            }
                        }
                    }
                    if (isShowMessage) {
                        _event.emit(WalletAuthenticationEvent.ForceSyncSuccess(signatures.size == tokens.size))
                    }
                    _state.update { it.copy(signatures = signatures) }
                }
            }
        }
    }

    private suspend fun loadPsbt() {
        val result = getDummyTxFromPsbt(
            GetDummyTxFromPsbt.Param(
                walletId = args.walletId,
                psbt = dataToSign.value
            )
        )
        if (result.isSuccess) {
            // hard code isReceive false I have no idea why first time it become true from libnunchuk
            _state.update {
                it.copy(transaction = result.getOrThrow().copy(isReceive = false))
            }
            getWalletDetails()
        } else {
            _event.emit(WalletAuthenticationEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun onSignerSelect(signerModel: SignerModel) = viewModelScope.launch {
        _state.update { it.copy(interactSingleSigner = null) }
        val singleSigner =
            _state.value.singleSigners.firstOrNull { it.masterFingerprint == signerModel.fingerPrint && it.derivationPath == signerModel.derivationPath }
                ?: return@launch
        savedStateHandle[EXTRA_CURRENT_INTERACT_SIGNER] = singleSigner
        _state.update { it.copy(interactSingleSigner = singleSigner) }
        when {
            signerModel.type == SignerType.NFC -> _event.emit(WalletAuthenticationEvent.ScanTapSigner)
            signerModel.type == SignerType.COLDCARD_NFC
                    || (signerModel.type == SignerType.HARDWARE
                    && signerModel.tags.contains(SignerTag.COLDCARD)) -> {
                _event.emit(WalletAuthenticationEvent.ScanColdCard)
            }

            signerModel.type == SignerType.SOFTWARE -> checkSoftwarePassPhrase(singleSigner)
            signerModel.type == SignerType.HARDWARE -> _event.emit(WalletAuthenticationEvent.CanNotSignHardwareKey)
            signerModel.type == SignerType.AIRGAP -> _event.emit(WalletAuthenticationEvent.ShowAirgapOption)
            signerModel.type == SignerType.PORTAL_NFC -> _event.emit(
                WalletAuthenticationEvent.RequestSignPortal(
                    fingerprint = signerModel.fingerPrint,
                    psbt = dataToSign.value
                )
            )

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
        cvc: String,
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
            val signatures = _state.value.signatures
            val validSignatures = _state.value.singleSigners.filter {
                transaction.signers[it.masterFingerprint] == true
                        && signatures.contains(it.masterFingerprint).not()
            }
            if (validSignatures.isEmpty()) {
                    _event.emit(WalletAuthenticationEvent.NoSignatureDetected)
                return@launch
            }
            validSignatures.forEach {
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
        }
    }

    fun handleSignPortalKey(psbt: String) {
        viewModelScope.launch {
            val signer = getInteractSingleSigner() ?: return@launch
            val result = getDummyTransactionSignatureUseCase(
                GetDummyTransactionSignatureUseCase.Param(
                    signer,
                    psbt
                )
            )
            if (result.isSuccess) {
                val signature = result.getOrThrow()
                if (signature.isEmpty()) {
                    _event.emit(WalletAuthenticationEvent.ShowError("Wallet has not registered to Portal yet"))
                } else {
                    handleSignatureResult(result, signer)
                }
            }
            handleSignatureResult(
                getDummyTransactionSignatureUseCase(
                    GetDummyTransactionSignatureUseCase.Param(
                        signer,
                        psbt
                    )
                ),
                signer
            )
        }
    }

    private fun checkSoftwarePassPhrase(singleSigner: SingleSigner) {
        viewModelScope.launch {
            if (singleSigner.hasMasterSigner) {
                getMasterSignerUseCase(singleSigner.masterSignerId).onSuccess {
                    if (it.device.needPassPhraseSent) {
                        _event.emit(WalletAuthenticationEvent.PromptPassphrase)
                    } else {
                        handleSignSoftware(singleSigner)
                    }
                }
            }
        }
    }

    private suspend fun handleSignSoftware(
        singleSigner: SingleSigner,
        isRequiredPassphrase: Boolean = false
    ) {
        val result = checkSignMessageUseCase(
            CheckSignMessageUseCase.Param(
                signer = singleSigner,
                messageToSign = dataToSign.value,
            )
        )
        if (isRequiredPassphrase) {
            clearSignerPassphraseUseCase(singleSigner.masterSignerId)
        }
        handleSignatureResult(result, singleSigner)
    }

    private suspend fun handleSignatureResult(
        result: Result<String>,
        singleSigner: SingleSigner,
    ) {
        if (result.isSuccess) {
            val signatures = _state.value.signatures.toMutableMap()
            val signature = result.getOrThrow()
            if (signature.isEmpty()) {
                _event.emit(WalletAuthenticationEvent.CanNotSignDummyTx)
                return
            }
            signatures[singleSigner.masterFingerprint] = signature
            if (!args.dummyTransactionId.isNullOrEmpty()) {
                uploadSignature(singleSigner.masterFingerprint, signature, signatures)
                    .onSuccess {
                        val status = _state.value.transactionStatus
                        if (status != TransactionStatus.CONFIRMED) {
                            _event.emit(WalletAuthenticationEvent.UploadSignatureSuccess(status))
                        }
                    }.onFailure {
                        if (it.isNoInternetException) {
                            _event.emit(WalletAuthenticationEvent.NoInternetConnectionToSign)
                        } else {
                            _event.emit(WalletAuthenticationEvent.ShowError(it.message.orUnknownError()))
                        }
                    }
            } else {
                if (signatures.size == args.requiredSignatures) {
                    _event.emit(WalletAuthenticationEvent.SignDummyTxSuccess(signatures))
                } else {
                    _state.update { it.copy(signatures = signatures) }
                }
            }
        } else {
            _event.emit(WalletAuthenticationEvent.SignFailed(singleSigner, result.exceptionOrNull()))
            _event.emit(WalletAuthenticationEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    private suspend fun uploadSignature(
        masterFingerprint: String,
        signature: String,
        signatures: MutableMap<String, String>,
    ): Result<DummyTransactionUpdate> {
        return updateGroupDummyTransactionUseCase(
            UpdateGroupDummyTransactionUseCase.Param(
                signatures = mapOf(masterFingerprint to signature),
                walletId = args.walletId,
                groupId = args.groupId.orEmpty(),
                transactionId = args.dummyTransactionId.orEmpty()
            )
        ).onSuccess { updateInfo ->
            _state.update {
                it.copy(
                    signatures = signatures,
                    pendingSignature = updateInfo.pendingSignatures,
                    transactionStatus = updateInfo.status
                )
            }
            if (updateInfo.status == TransactionStatus.CONFIRMED) {
                if (args.action == TargetAction.CLAIM_KEY.name) {
                    runCatching {
                        syncGroupWalletUseCase(args.groupId.orEmpty())
                    }
                }
                _event.emit(WalletAuthenticationEvent.SignDummyTxSuccess())
            }
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
                                .copy(cardId = cardIdManager.getCardId(it.masterSignerId)) else it.toModel()
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

    fun getDummyTransactionExtra(): Bundle = bundleOf(
        GlobalResultKey.EXTRA_DUMMY_TX_TYPE to _state.value.dummyTransactionType,
        GlobalResultKey.EXTRA_HEALTH_CHECK_XFP to _state.value.enabledSigners.firstOrNull()
    )

    fun finalizeDummyTransaction(isGoBack: Boolean) {
        val isDraft = state.value.isDraft
        viewModelScope.launch {
            if (isDraft.not()) {
                _event.emit(WalletAuthenticationEvent.FinalizeDummyTxSuccess(isGoBack))
                return@launch
            }
            _event.emit(WalletAuthenticationEvent.Loading(true))
            finalizeDummyTransactionUseCase(
                FinalizeDummyTransactionUseCase.Params(
                    groupId = args.groupId.orEmpty(),
                    walletId = args.walletId,
                    dummyTransactionId = args.dummyTransactionId.orEmpty()
                )
            ).onSuccess {
                _event.emit(WalletAuthenticationEvent.Loading(false))
                _event.emit(WalletAuthenticationEvent.FinalizeDummyTxSuccess(isGoBack))
            }.onFailure {
                _event.emit(WalletAuthenticationEvent.Loading(false))
                _event.emit(WalletAuthenticationEvent.ShowError(it.message.orUnknownError()))
            }
        }
    }

    fun deleteDummyTransaction() {
        viewModelScope.launch(NonCancellable) {
            val isDraft = state.value.isDraft
            if (!args.dummyTransactionId.isNullOrEmpty() && isDraft) {
                deleteGroupDummyTransactionUseCase(
                    DeleteGroupDummyTransactionUseCase.Param(
                        groupId = args.groupId.orEmpty(),
                        walletId = args.walletId,
                        transactionId = args.dummyTransactionId.orEmpty()
                    )
                )
            }
        }
    }

    private fun isValidSigner(type: SignerType, authenticationType: String): Boolean {
        if (authenticationType == VerificationType.SIGN_DUMMY_TX && type == SignerType.AIRGAP) return true
        return type == SignerType.SOFTWARE
                || type == SignerType.HARDWARE
                || type == SignerType.NFC
                || type == SignerType.COLDCARD_NFC
                || type == SignerType.FOREIGN_SOFTWARE
                || type == SignerType.PORTAL_NFC
    }

    fun getDummyTransactionType() = _state.value.dummyTransactionType
    fun handlePassphrase(passphrase: String) {
        val currentSigner = getInteractSingleSigner() ?: return
        viewModelScope.launch {
            sendSignerPassphrase.execute(currentSigner.masterSignerId, passphrase)
                .flowOn(Dispatchers.IO)
                .onException {
                    _event.emit(WalletAuthenticationEvent.ShowError(it.message.orUnknownError()))
                }
                .collect { handleSignSoftware(currentSigner, true) }
        }
    }

    val signedSuccessMessage: String
        get() = if (state.value.transactionStatus != TransactionStatus.CONFIRMED) {
            application.getString(R.string.nc_transaction_updated)
        } else when (state.value.dummyTransactionType) {
            DummyTransactionType.REQUEST_INHERITANCE_PLANNING -> application.getString(R.string.nc_inheritance_planning_request_approved)
            DummyTransactionType.UPDATE_SERVER_KEY -> application.getString(R.string.nc_policy_updated)
            DummyTransactionType.HEALTH_CHECK_PENDING,
            DummyTransactionType.HEALTH_CHECK_REQUEST,
            -> application.getString(
                R.string.nc_txt_run_health_check_success_event,
                getInteractSingleSigner()?.name.orEmpty()
            )

            DummyTransactionType.CREATE_RECURRING_PAYMENT -> application.getString(R.string.nc_the_recurring_payment_has_been_approved)
            DummyTransactionType.CANCEL_RECURRING_PAYMENT -> application.getString(R.string.nc_pending_cancellation_has_been_cancelled)
            DummyTransactionType.UPDATE_SECURITY_QUESTIONS -> application.getString(R.string.nc_security_questions_updated)
            else -> ""
        }

    companion object {
        private const val EXTRA_CURRENT_INTERACT_SIGNER = "EXTRA_CURRENT_INTERACT_SIGNER"
    }
}