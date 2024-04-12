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

package com.nunchuk.android.transaction.components.details

import android.app.Application
import android.net.Uri
import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.ExportPsbtToMk4UseCase
import com.nunchuk.android.core.domain.GetRawTransactionUseCase
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.domain.ImportTransactionFromMk4UseCase
import com.nunchuk.android.core.domain.SignRoomTransactionByTapSignerUseCase
import com.nunchuk.android.core.domain.SignTransactionByTapSignerUseCase
import com.nunchuk.android.core.domain.membership.CancelScheduleBroadcastTransactionUseCase
import com.nunchuk.android.core.domain.membership.RequestSignatureTransactionUseCase
import com.nunchuk.android.core.network.ApiErrorCode
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.canBroadCast
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.isNoInternetException
import com.nunchuk.android.core.util.isPending
import com.nunchuk.android.core.util.isPendingConfirm
import com.nunchuk.android.core.util.isPendingSignatures
import com.nunchuk.android.core.util.isRejected
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.listener.BlockListener
import com.nunchuk.android.listener.TransactionListener
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.Device
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.joinKeys
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.BroadcastTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.CancelScheduleBroadcastTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.DeleteTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ExportToFileSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ExportTransactionToMk4Success
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.GetRawTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ImportTransactionFromMk4Success
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ImportTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.LoadingEvent
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.NfcLoadingEvent
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.NoInternetConnection
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.PromptInputPassphrase
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.PromptTransactionOptions
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.SignTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.TransactionDetailsError
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.TransactionError
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.UpdateTransactionMemoFailed
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.UpdateTransactionMemoSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ViewBlockchainExplorer
import com.nunchuk.android.transaction.usecase.GetBlockchainExplorerUrlUseCase
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.BroadcastTransactionUseCase
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.DeleteTransactionUseCase
import com.nunchuk.android.usecase.ExportTransactionUseCase
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import com.nunchuk.android.usecase.GetTransactionFromNetworkUseCase
import com.nunchuk.android.usecase.GetTransactionUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.ImportTransactionUseCase
import com.nunchuk.android.usecase.SendSignerPassphrase
import com.nunchuk.android.usecase.SignTransactionUseCase
import com.nunchuk.android.usecase.UpdateTransactionMemo
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.coin.GetCoinsFromTxInputsUseCase
import com.nunchuk.android.usecase.membership.SignServerTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.BroadcastRoomTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.GetPendingTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.SignRoomTransactionUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.TransactionException
import com.nunchuk.android.utils.onException
import com.nunchuk.android.utils.retrieveInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
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
    private val importTransactionUseCase: ImportTransactionUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val getRawTransactionUseCase: GetRawTransactionUseCase,
    private val requestSignatureTransactionUseCase: RequestSignatureTransactionUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val byzantineGroupUtils: ByzantineGroupUtils,
    private val getCoinsFromTxInputsUseCase: GetCoinsFromTxInputsUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val application: Application,
) : NunchukViewModel<TransactionDetailsState, TransactionDetailsEvent>() {

    private var walletId: String = ""
    private var txId: String = ""
    private var initEventId: String = ""
    private var roomId: String = ""
    private var initTransaction: Transaction? = null

    private var masterSigners: List<MasterSigner> = emptyList()

    private var contacts: List<Contact> = emptyList()

    private var initNumberOfSignedKey = INVALID_NUMBER_OF_SIGNED

    override val initialState = TransactionDetailsState()

    private var reloadTransactionJob: Job? = null
    private var getTransactionJob: Job? = null

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
            pushEventManager.event.collect { event ->
                if (event is PushEvent.ServerTransactionEvent) {
                    if (event.transactionId == txId) {
                        Timber.d("ServerTransactionEvent")
                        delay(2000L)
                        getTransactionInfo()
                    }
                } else if (event is PushEvent.TransactionCancelled) {
                    if (event.transactionId == txId) {
                        setEvent(DeleteTransactionSuccess(true))
                    }
                } else if (event is PushEvent.SignedChanged) {
                    if (getState().signers.any { signer -> signer.fingerPrint == event.xfp }) {
                        loadWallet()
                    }
                }
            }
        }
        viewModelScope.launch {
            state.asFlow()
                .map { it.transaction.inputs }
                .filter { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect {
                    getCoinsFromTxInputsUseCase(GetCoinsFromTxInputsUseCase.Params(walletId, it))
                        .onSuccess { coins ->
                            updateState { copy(txInputCoins = coins) }
                        }
                }
        }
    }

    fun init(
        walletId: String,
        txId: String,
        initEventId: String,
        roomId: String,
        transaction: Transaction?,
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
        listenTransactionChanged()
        getAllTags()
        getAllCoins()
        getGroupMembers()
    }

    private fun getGroupMembers() {
        val groupId = assistedWalletManager.getGroupId(walletId) ?: return
        viewModelScope.launch {
            getGroupUseCase(GetGroupUseCase.Params(groupId = groupId))
                .map { it.getOrElse { null } }
                .distinctUntilChanged()
                .collect { group ->
                    val members = group?.members.orEmpty()
                        .filter {
                            it.role != AssistedWalletRole.OBSERVER.name
                                    && isMatchingEmailOrUserName(it.emailOrUsername).not()
                                    && it.isPendingRequest().not()
                        }
                    updateState {
                        copy(
                            members = members,
                            userRole = byzantineGroupUtils.getCurrentUserRole(group).toRole
                        )
                    }
                }
        }
    }

    fun getAllTags() {
        viewModelScope.launch {
            getAllTagsUseCase(walletId).onSuccess { allTags ->
                updateState { copy(tags = allTags.associateBy { tag -> tag.id }) }
            }
        }
    }

    fun getAllCoins() {
        viewModelScope.launch {
            getAllCoinUseCase(walletId).onSuccess { coins ->
                updateState { copy(coins = coins.filter { it.txid == txId }) }
            }
        }
    }

    private fun listenTransactionChanged() {
        if (isAssistedWallet()) {
            viewModelScope.launch {
                state.asFlow().collect {
                    if (it.transaction.txId.isNotEmpty()) {
                        val signedCount = it.transaction.signers.count { entry -> entry.value }
                        if (initNumberOfSignedKey == INVALID_NUMBER_OF_SIGNED) {
                            initNumberOfSignedKey = signedCount
                        } else if (signedCount > initNumberOfSignedKey) {
                            initNumberOfSignedKey = signedCount
                            if (signedCount > 0 && (it.transaction.status.isPendingSignatures() || it.transaction.status.canBroadCast())) {
                                requestServerSignTransaction(it.transaction.psbt)
                            }
                        }
                        val signedTime = it.serverTransaction?.signedInMilis ?: 0L
                        handleSignTime(signedTime)
                    }
                }
            }
        }
    }

    private fun handleSignTime(signedTime: Long) {
        if (signedTime > 0L && signedTime > System.currentTimeMillis()) {
            reloadTransactionJob?.cancel()
            reloadTransactionJob = viewModelScope.launch {
                val delay = signedTime - System.currentTimeMillis() + 3000L
                delay(delay)
                getTransactionInfo()
            }
        }
    }

    private fun requestServerSignTransaction(psbt: String) {
        viewModelScope.launch {
            signServerTransactionUseCase(
                SignServerTransactionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    txId = txId,
                    psbt = psbt
                )
            ).onSuccess { extendedTransaction ->
                setEvent(
                    SignTransactionSuccess(
                        status = extendedTransaction.transaction.status,
                        serverSigned = isSignByServerKey(extendedTransaction.transaction)
                    )
                )
                updateState {
                    copy(
                        transaction = extendedTransaction.transaction,
                        serverTransaction = extendedTransaction.serverTransaction
                    )
                }
            }.onFailure {
                if (it.isNoInternetException) {
                    setEvent(NoInternetConnection)
                } else {
                    setEvent(TransactionDetailsError(it.message.orUnknownError()))
                }
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
        savedStateHandle[KEY_CURRENT_SIGNER] = signer
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
            val result = updateTransactionMemo(
                UpdateTransactionMemo.Data(
                    assistedWalletManager.getGroupId(walletId),
                    walletId,
                    isAssistedWallet(),
                    txId,
                    newMemo
                )
            )
            if (result.isSuccess) {
                updateTransaction(getState().transaction.copy(memo = newMemo))
                setEvent(UpdateTransactionMemoSuccess(newMemo))
            } else {
                setEvent(UpdateTransactionMemoFailed(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun getTransactionFromNetwork() {
        if (getTransactionJob?.isActive == true) return
        getTransactionJob = viewModelScope.launch {
            val result = getTransactionFromNetworkUseCase(txId)
            if (result.isSuccess) {
                updateState { copy(transaction = result.getOrThrow()) }
            }
        }
    }

    private fun loadLocalTransaction() {
        if (getTransactionJob?.isActive == true) return
        getTransactionJob = viewModelScope.launch {
            getTransactionUseCase.execute(
                groupId = assistedWalletManager.getGroupId(walletId),
                walletId = walletId,
                txId = txId,
                isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
            ).flowOn(IO).onException {
                if ((it as? NunchukApiException)?.code == ApiErrorCode.TRANSACTION_CANCEL) {
                    handleDeleteTransactionEvent(isCancel = true, onlyLocal = true)
                } else if (it.isNoInternetException.not()) {
                    setEvent(TransactionDetailsError(it.message.orEmpty()))
                }
            }.collect {
                updateTransaction(
                    transaction = it.transaction,
                    serverTransaction = it.serverTransaction,
                )
            }
        }
    }

    fun updateServerTransaction(serverTransaction: ServerTransaction?) {
        updateState { copy(serverTransaction = serverTransaction) }
    }

    private fun updateTransaction(
        transaction: Transaction,
        serverTransaction: ServerTransaction? = getState().serverTransaction,
    ) {
        updateState {
            copy(
                transaction = transaction,
                serverTransaction = serverTransaction,
            )
        }
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
                isRejected = status.isRejected(),
                canBroadcast = status.canBroadCast(),
            )
        )
    }

    fun handleDeleteTransactionEvent(isCancel: Boolean = true, onlyLocal: Boolean = false) {
        viewModelScope.launch {
            setEvent(LoadingEvent)
            val result = deleteTransactionUseCase(
                DeleteTransactionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(walletId),
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
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    transactionId = txId,
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

    fun handleSignSoftwareKey(signer: SignerModel) {
        viewModelScope.launch {
            val fingerPrint = signer.fingerPrint
            val device =
                masterSigners.firstOrNull { it.device.masterFingerprint == fingerPrint }?.device
                    ?: return@launch
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
    }

    private fun signTransaction(device: Device, signerId: String) {
        if (isSharedTransaction()) {
            signRoomTransaction(device, signerId)
        } else {
            signPersonalTransaction(device, signerId)
        }
    }

    private fun isSharedTransaction() = roomId.isNotEmpty()

    fun exportTransactionToFile() {
        viewModelScope.launch {
            setEvent(LoadingEvent)
            when (val result = createShareFileUseCase.execute("${walletId}_${txId}.psbt")) {
                is Success -> exportTransaction(result.data)
                is Error -> {
                    val message =
                        "${result.exception.messageOrUnknownError()},walletId::$walletId,txId::$txId"
                    setEvent(TransactionError(message))
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
                    setEvent(TransactionError(message))
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
        val signer = savedStateHandle.get<SignerModel>(KEY_CURRENT_SIGNER) ?: return
        viewModelScope.launch {
            setEvent(LoadingEvent)
            val result = importTransactionFromMk4UseCase(
                ImportTransactionFromMk4UseCase.Data(
                    walletId, records, initEventId, signer.fingerPrint
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
                    isoDep, inputCvc, initEventId
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

    fun currentSigner() = savedStateHandle.get<SignerModel>(KEY_CURRENT_SIGNER)

    private fun fireSignError(e: Throwable?) {
        val message = "${e?.message.orEmpty()},walletId::$walletId,txId::$txId"
        setEvent(TransactionDetailsError(message, e))
        CrashlyticsReporter.recordException(TransactionException(message))
    }

    fun isAssistedWallet() = assistedWalletManager.isActiveAssistedWallet(walletId)
    private fun isIronHandWallet() = getState().signers.size == 3 && getState().signers.any { it.type == SignerType.SERVER }
    fun isSupportScheduleBroadcast() = isAssistedWallet() && !isIronHandWallet()

    fun isScheduleBroadcast() = (getState().serverTransaction?.broadcastTimeInMilis ?: 0L) > 0L

    fun getMembers() = getState().members

    fun isInheritanceSigner(xfp: String): Boolean =
        masterSigners.find { it.device.masterFingerprint == xfp }?.tags?.contains(SignerTag.INHERITANCE) == true

    fun importTransactionViaFile(walletId: String, uri: Uri) {
        viewModelScope.launch {
            val file = withContext(dispatcher) {
                getFileFromUri(application.contentResolver, uri, application.cacheDir)
            } ?: return@launch
            importTransactionUseCase(
                ImportTransactionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    filePath = file.absolutePath,
                    isAssistedWallet = isAssistedWallet()
                )
            ).onSuccess {
                getTransactionInfo()
                event(ImportTransactionSuccess)
            }.onFailure {
                event(TransactionError(it.readableMessage()))
            }
        }
    }

    fun getRawTransaction() = viewModelScope.launch {
        val result = getRawTransactionUseCase(GetRawTransactionUseCase.Param(walletId, txId))
        if (result.isSuccess) {
            setEvent(GetRawTransactionSuccess(result.getOrThrow()))
        } else {
            setEvent(TransactionError(result.exceptionOrNull()?.readableMessage().orUnknownError()))
        }
    }

    fun requestSignatureTransaction(membershipId: String) = viewModelScope.launch {
        setEvent(LoadingEvent)
        requestSignatureTransactionUseCase(
            RequestSignatureTransactionUseCase.Param(
                groupId = assistedWalletManager.getGroupId(walletId).orEmpty(),
                walletId = walletId,
                transactionId = txId,
                membershipId = membershipId
            )
        ).onSuccess {
            setEvent(TransactionDetailsEvent.RequestSignatureTransactionSuccess)
        }.onFailure {
            setEvent(TransactionError(it.readableMessage().orUnknownError()))
        }
    }

    fun allTags() = getState().tags
    fun coins() = getState().coins

    fun getUserRole() = getState().userRole

    private fun isSignByServerKey(transaction: Transaction): Boolean {
        val fingerPrint =
            getState().signers.find { it.type == SignerType.SERVER }?.fingerPrint.orEmpty()
        return transaction.signers[fingerPrint] == true
    }

    fun toggleShowInputCoin() =
        updateState { copy(isShowInputCoin = getState().isShowInputCoin.not()) }

    private fun isMatchingEmailOrUserName(emailOrUsername: String) =
        emailOrUsername == accountManager.getAccount().email
                || emailOrUsername == accountManager.getAccount().username

    fun getWalletPlan() = assistedWalletManager.getWalletPlan(walletId)

    companion object {
        private const val INVALID_NUMBER_OF_SIGNED = -1
        private const val KEY_CURRENT_SIGNER = "current_signer"
    }
}