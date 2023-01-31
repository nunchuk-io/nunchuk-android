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

package com.nunchuk.android.transaction.components.details

import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.*
import com.nunchuk.android.core.domain.membership.CancelScheduleBroadcastTransactionUseCase
import com.nunchuk.android.core.network.ApiErrorCode
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.*
import com.nunchuk.android.listener.BlockListener
import com.nunchuk.android.listener.TransactionListener
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.*
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.*
import com.nunchuk.android.transaction.usecase.GetBlockchainExplorerUrlUseCase
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.*
import com.nunchuk.android.usecase.membership.SignServerTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.BroadcastRoomTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.GetPendingTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.SignRoomTransactionUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.TransactionException
import com.nunchuk.android.utils.onException
import com.nunchuk.android.utils.retrieveInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class TransactionDetailsViewModel @Inject constructor(
    private val getBlockchainExplorerUrlUseCase: GetBlockchainExplorerUrlUseCase,
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val signTransactionUseCase: SignTransactionUseCase,
    private val signRoomTransactionUseCase: SignRoomTransactionUseCase,
    private val broadcastTransactionUseCase: BroadcastTransactionUseCase,
    private val broadcastRoomTransactionUseCase: BroadcastRoomTransactionUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportTransactionUseCase: ExportTransactionUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val signTransactionByTapSignerUseCase: SignTransactionByTapSignerUseCase,
    private val signRoomTransactionByTapSignerUseCase: SignRoomTransactionByTapSignerUseCase,
    private val updateTransactionMemo: UpdateTransactionMemo,
    private val exportPsbtToMk4UseCase: ExportPsbtToMk4UseCase,
    private val importTransactionFromMk4UseCase: ImportTransactionFromMk4UseCase,
    private val getPendingTransactionUseCase: GetPendingTransactionUseCase,
    private val getTransactionFromNetworkUseCase: GetTransactionFromNetworkUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val accountManager: AccountManager,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    private val pushEventManager: PushEventManager,
    private val signServerTransactionUseCase: SignServerTransactionUseCase,
    private val cancelScheduleBroadcastTransactionUseCase: CancelScheduleBroadcastTransactionUseCase,
) : NunchukViewModel<TransactionDetailsState, TransactionDetailsEvent>() {

    private var walletId: String = ""
    private var txId: String = ""
    private var initEventId: String = ""
    private var roomId: String = ""
    private var initTransaction: Transaction? = null

    private var masterSigners: List<MasterSigner> = emptyList()

    private var contacts: List<Contact> = emptyList()

    private var currentSigner: SignerModel? = null

    private var initNumberOfSignedKey = INVALID_NUMBER_OF_SIGNED

    override val initialState = TransactionDetailsState()

    init {
        viewModelScope.launch {
            BlockListener.getBlockChainFlow().collect {
                getTransactionInfo()
            }
        }
        viewModelScope.launch {
            TransactionListener.transactionUpdateFlow.collect {
                if (it.txId == txId) {
                    getTransactionInfo()
                }
            }
        }
        viewModelScope.launch {
            pushEventManager.event.filterIsInstance<PushEvent.ServerTransactionEvent>().collect {
                if (it.transactionId == txId) {
                    getTransactionInfo()
                }
            }
        }
    }

    fun init(
        walletId: String,
        txId: String,
        initEventId: String,
        roomId: String,
        transaction: Transaction?
    ) {
        this.walletId = walletId
        this.txId = txId
        this.initEventId = initEventId
        this.roomId = roomId
        this.initTransaction = transaction

        if (isSharedTransaction()) {
            getContacts()
        } else {
            loadWallet()
        }
        loadInitEventIdIfNeed()
        initTransaction?.let {
            updateState { copy(transaction = it) }
        }
        loadMasterSigner()
        listenSignKey()
    }

    private fun listenSignKey() {
        if (isAssistedWallet()) {
            viewModelScope.launch {
                state.asFlow().collect {
                    if (it.transaction.txId.isNotEmpty()) {
                        val signedCount = it.transaction.signers.count { entry -> entry.value }
                        if (initNumberOfSignedKey == INVALID_NUMBER_OF_SIGNED) {
                            initNumberOfSignedKey = signedCount
                        } else if (signedCount > initNumberOfSignedKey) {
                            initNumberOfSignedKey = signedCount
                            if (signedCount > 0) {
                                requestServerSignTransaction(it.transaction.psbt)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestServerSignTransaction(psbt: String) {
        viewModelScope.launch {
            val result = signServerTransactionUseCase(
                SignServerTransactionUseCase.Param(
                    walletId,
                    txId,
                    psbt
                )
            )
            if (result.isSuccess) {
                val extendedTransaction = result.getOrThrow()
                setEvent(
                    SignTransactionSuccess(
                        isAssistedWallet = true,
                        status = extendedTransaction.transaction.status
                    )
                )
                updateState {
                    copy(
                        transaction = extendedTransaction.transaction,
                        serverTransaction = extendedTransaction.serverTransaction
                    )
                }
            } else {
                setEvent(TransactionDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun loadMasterSigner() {
        viewModelScope.launch {
            getMasterSignersUseCase.execute().collect {
                masterSigners = it
            }
        }
    }

    private fun loadWallet() {
        if (walletId.isEmpty()) return
        viewModelScope.launch {
            getWalletUseCase.execute(walletId).collect {
                val account = accountManager.getAccount()
                val signers = it.wallet.signers.map { signer ->
                    if (signer.type == SignerType.NFC) {
                        signer.toModel()
                            .copy(cardId = getTapSignerStatusByIdUseCase(signer.masterSignerId).getOrNull()?.ident.orEmpty())
                    } else {
                        signer.toModel()
                    }
                }
                if (it.roomWallet != null) {
                    updateState {
                        copy(signers = it.roomWallet?.joinKeys().orEmpty().map { key ->
                            key.retrieveInfo(
                                key.chatId == account.chatId, signers, contacts
                            )
                        })
                    }
                } else {
                    updateState { copy(signers = signers) }
                }
            }
        }
    }

    fun setCurrentSigner(signer: SignerModel) {
        currentSigner = signer
    }

    private fun loadInitEventIdIfNeed() {
        if (initEventId.isEmpty() && roomId.isNotEmpty()) {
            setEvent(LoadingEvent)
            viewModelScope.launch {
                val result =
                    getPendingTransactionUseCase(GetPendingTransactionUseCase.Data(roomId, txId))
                if (result.isSuccess) {
                    initEventId = result.getOrThrow().initEventId
                }
            }
        }
    }

    fun getInitEventId(): String {
        return initEventId
    }

    private fun getContacts() {
        viewModelScope.launch {
            getContactsUseCase.execute().catch { contacts = emptyList() }.collect {
                contacts = it
                loadWallet()
            }
        }
    }

    fun getTransactionInfo() {
        setEvent(LoadingEvent)
        if (initTransaction != null) {
            getTransactionFromNetwork()
        } else {
            loadLocalTransaction()
        }
    }

    fun getTransaction() = getState().transaction

    fun updateTransactionMemo(newMemo: String) {
        viewModelScope.launch {
            setEvent(LoadingEvent)
            val result = updateTransactionMemo(UpdateTransactionMemo.Data(walletId, txId, newMemo))
            if (result.isSuccess) {
                setEvent(UpdateTransactionMemoSuccess(newMemo))
            } else {
                setEvent(UpdateTransactionMemoFailed(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun getTransactionFromNetwork() {
        viewModelScope.launch {
            val result = getTransactionFromNetworkUseCase(txId)
            if (result.isSuccess) {
                updateState { copy(transaction = result.getOrThrow()) }
            }
        }
    }

    private fun loadLocalTransaction() {
        viewModelScope.launch {
            getTransactionUseCase.execute(
                walletId,
                txId,
                assistedWalletManager.isActiveAssistedWallet(walletId)
            ).flowOn(IO)
                .onException {
                    if ((it as? NunchukApiException)?.code == ApiErrorCode.TRANSACTION_CANCEL) {
                        handleDeleteTransactionEvent(isCancel = true, onlyLocal = true)
                    } else {
                        setEvent(TransactionDetailsError(it.message.orEmpty()))
                    }
                }
                .collect {
                    updateTransaction(it.transaction, it.serverTransaction)
                }
        }
    }

    fun updateServerTransaction(serverTransaction: ServerTransaction?) {
        updateState { copy(serverTransaction = serverTransaction) }
    }

    private fun updateTransaction(
        transaction: Transaction,
        serverTransaction: ServerTransaction? = getState().serverTransaction
    ) {
        updateState { copy(transaction = transaction, serverTransaction = serverTransaction) }
    }

    fun handleViewMoreEvent() {
        updateState { copy(viewMore = !viewMore) }
    }

    fun handleBroadcastEvent() {
        if (isSharedTransaction()) {
            broadcastSharedTransaction()
        } else {
            broadcastPersonalTransaction()
        }
    }

    private fun broadcastPersonalTransaction() {
        viewModelScope.launch {
            setEvent(LoadingEvent)
            broadcastTransactionUseCase.execute(walletId, txId).flowOn(IO)
                .onException { setEvent(TransactionDetailsError(it.message.orEmpty())) }.collect {
                    updateTransaction(it)
                    setEvent(BroadcastTransactionSuccess())
                }
        }
    }

    private fun broadcastSharedTransaction() {
        viewModelScope.launch {
            setEvent(LoadingEvent)
            broadcastRoomTransactionUseCase.execute(initEventId).flowOn(IO)
                .onException { setEvent(TransactionDetailsError(it.message.orEmpty())) }
                .collect { setEvent(BroadcastTransactionSuccess(roomId)) }
        }
    }

    fun handleViewBlockchainEvent() = viewModelScope.launch {
        val result = getBlockchainExplorerUrlUseCase(txId)
        if (result.isSuccess) {
            setEvent(ViewBlockchainExplorer(result.getOrThrow()))
        } else {
            setEvent(TransactionDetailsError(result.exceptionOrNull()?.message.orEmpty()))
        }
    }

    fun handleMenuMoreEvent() {
        val status = getState().transaction.status
        setEvent(
            PromptTransactionOptions(
                isPendingTransaction = status.isPending(),
                isPendingConfirm = status.isPendingConfirm(),
                isRejected = status.isRejected()
            )
        )
    }

    fun handleDeleteTransactionEvent(isCancel: Boolean = true, onlyLocal: Boolean = false) {
        viewModelScope.launch {
            val result = deleteTransactionUseCase(
                DeleteTransactionUseCase.Param(
                    walletId = walletId,
                    txId = txId,
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId) && !onlyLocal
                )
            )
            if (result.isSuccess) {
                setEvent(DeleteTransactionSuccess(isCancel))
            } else {
                setEvent(TransactionDetailsError("${result.exceptionOrNull()?.message.orUnknownError()}, walletId::$walletId, txId::$txId"))
            }
        }
    }

    fun cancelScheduleBroadcast() {
        viewModelScope.launch {
            val result = cancelScheduleBroadcastTransactionUseCase(
                CancelScheduleBroadcastTransactionUseCase.Param(
                    walletId, txId
                )
            )
            if (result.isSuccess) {
                updateState { copy(serverTransaction = result.getOrThrow()) }
                setEvent(CancelScheduleBroadcastTransactionSuccess)
            } else {
                setEvent(TransactionDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun handleSignEvent(signer: SignerModel) {
        if (signer.software) {
            viewModelScope.launch {
                val fingerPrint = signer.fingerPrint
                val device =
                    masterSigners.first { it.device.masterFingerprint == fingerPrint }.device
                if (device.needPassPhraseSent) {
                    setEvent(PromptInputPassphrase {
                        viewModelScope.launch {
                            sendSignerPassphrase.execute(signer.id, it).flowOn(IO)
                                .onException { setEvent(TransactionDetailsError("${it.message.orEmpty()},walletId::$walletId,txId::$txId")) }
                                .collect { signTransaction(device, signer.id) }
                        }
                    })
                } else {
                    signTransaction(device, signer.id)
                }
            }
        } else {
            setEvent(
                PromptTransactionOptions(
                    isPendingTransaction = true,
                    isPendingConfirm = false,
                    isRejected = false, masterFingerPrint = signer.fingerPrint
                )
            )
        }
    }

    private fun signTransaction(device: Device, signerId: String) {
        if (isSharedTransaction()) {
            signRoomTransaction(device, signerId)
        } else {
            signPersonalTransaction(device, signerId)
        }
    }

    fun isSharedTransaction() = roomId.isNotEmpty()

    fun exportTransactionToFile() {
        viewModelScope.launch {
            setEvent(LoadingEvent)
            when (val result = createShareFileUseCase.execute("${walletId}_${txId}.psbt")) {
                is Success -> exportTransaction(result.data)
                is Error -> {
                    val message =
                        "${result.exception.messageOrUnknownError()},walletId::$walletId,txId::$txId"
                    setEvent(ExportTransactionError(message))
                    CrashlyticsReporter.recordException(TransactionException(message))
                }
            }
        }
    }

    private fun exportTransaction(filePath: String) {
        viewModelScope.launch {
            when (val result = exportTransactionUseCase.execute(walletId, txId, filePath)) {
                is Success -> setEvent(ExportToFileSuccess(filePath))
                is Error -> {
                    val message =
                        "${result.exception.messageOrUnknownError()},walletId::$walletId,txId::$txId"
                    setEvent(ExportTransactionError(message))
                    CrashlyticsReporter.recordException(TransactionException(message))
                }
            }
        }
    }

    private fun signRoomTransaction(device: Device, signerId: String) {
        viewModelScope.launch {
            signRoomTransactionUseCase.execute(initEventId = initEventId, device = device, signerId)
                .flowOn(IO).onException {
                    fireSignError(it)
                }.collect { setEvent(SignTransactionSuccess(roomId)) }
        }
    }

    private fun signPersonalTransaction(device: Device, signerId: String) {
        viewModelScope.launch {
            val isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
            val result = signTransactionUseCase(
                SignTransactionUseCase.Param(
                    walletId = walletId,
                    txId = txId,
                    device = device,
                    signerId = signerId,
                    isAssistedWallet = isAssistedWallet
                )
            )
            if (result.isSuccess) {
                updateTransaction(
                    transaction = result.getOrThrow(),
                )
                setEvent(SignTransactionSuccess())
            } else {
                fireSignError(result.exceptionOrNull())
            }
        }
    }

    fun handleSignByTapSigner(isoDep: IsoDep?, inputCvc: String) {
        isoDep ?: return
        if (isSharedTransaction()) {
            signRoomTransactionTapSigner(isoDep, inputCvc)
        } else {
            signPersonTapSignerTransaction(isoDep, inputCvc)
        }
    }

    fun handleExportTransactionToMk4(ndef: Ndef) {
        viewModelScope.launch {
            setEvent(NfcLoadingEvent(true))
            val result = exportPsbtToMk4UseCase(ExportPsbtToMk4UseCase.Data(walletId, txId, ndef))
            if (result.isSuccess) {
                setEvent(ExportTransactionToMk4Success)
            } else {
                fireSignError(result.exceptionOrNull())
            }
        }
    }

    fun handleImportTransactionFromMk4(records: List<NdefRecord>) {
        val signer = currentSigner ?: return
        viewModelScope.launch {
            setEvent(LoadingEvent)
            val result = importTransactionFromMk4UseCase(
                ImportTransactionFromMk4UseCase.Data(
                    walletId,
                    records,
                    initEventId,
                    signer.fingerPrint
                )
            )
            val transaction = result.getOrNull()
            if (result.isSuccess && transaction != null) {
                updateState { copy(transaction = transaction) }
                setEvent(ImportTransactionFromMk4Success)
            } else {
                fireSignError(result.exceptionOrNull())
            }
        }
    }

    private fun signRoomTransactionTapSigner(isoDep: IsoDep, inputCvc: String) {
        viewModelScope.launch {
            setEvent(NfcLoadingEvent())
            val result = signRoomTransactionByTapSignerUseCase(
                SignRoomTransactionByTapSignerUseCase.Data(
                    isoDep,
                    inputCvc,
                    initEventId
                )
            )
            if (result.isSuccess) {
                setEvent(SignTransactionSuccess(roomId))
            } else {
                fireSignError(result.exceptionOrNull())
            }
        }
    }

    private fun signPersonTapSignerTransaction(isoDep: IsoDep, inputCvc: String) {
        viewModelScope.launch {
            setEvent(NfcLoadingEvent())
            val isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
            val result = signTransactionByTapSignerUseCase(
                SignTransactionByTapSignerUseCase.Data(
                    isoDep = isoDep,
                    cvc = inputCvc,
                    walletId = walletId,
                    txId = txId,
                    isAssistedWallet = isAssistedWallet
                )
            )
            if (result.isSuccess) {
                updateTransaction(
                    transaction = result.getOrThrow(),
                )
                setEvent(SignTransactionSuccess())
            } else {
                fireSignError(result.exceptionOrNull())
            }
        }
    }

    private fun fireSignError(e: Throwable?) {
        val message = "${e?.message.orEmpty()},walletId::$walletId,txId::$txId"
        setEvent(TransactionDetailsError(message, e))
        CrashlyticsReporter.recordException(TransactionException(message))
    }

    fun isAssistedWallet() = assistedWalletManager.isActiveAssistedWallet(walletId)

    fun isScheduleBroadcast() = (getState().serverTransaction?.broadcastTimeInMilis ?: 0L) > 0L

    fun isInheritanceSigner(xfp: String): Boolean = masterSigners.find { it.device.masterFingerprint == xfp }?.tags?.contains(SignerTag.INHERITANCE) == true

    companion object {
        private const val INVALID_NUMBER_OF_SIGNED = -1
    }
}