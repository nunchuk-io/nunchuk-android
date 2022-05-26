package com.nunchuk.android.transaction.components.details

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.signer.toSignerModel
import com.nunchuk.android.core.util.isPending
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.model.*
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.*
import com.nunchuk.android.transaction.usecase.GetBlockchainExplorerUrlUseCase
import com.nunchuk.android.usecase.*
import com.nunchuk.android.usecase.room.transaction.BroadcastRoomTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.SignRoomTransactionUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class TransactionDetailsViewModel @Inject constructor(
    private val getBlockchainExplorerUrlUseCase: GetBlockchainExplorerUrlUseCase,
    private val getAllSignersUseCase: GetCompoundSignersUseCase,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val signTransactionUseCase: SignTransactionUseCase,
    private val signRoomTransactionUseCase: SignRoomTransactionUseCase,
    private val broadcastTransactionUseCase: BroadcastTransactionUseCase,
    private val broadcastRoomTransactionUseCase: BroadcastRoomTransactionUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportTransactionUseCase: ExportTransactionUseCase,
    private val getRoomWalletUseCase: GetRoomWalletUseCase,
    private val getContactsUseCase: GetContactsUseCase
) : NunchukViewModel<TransactionDetailsState, TransactionDetailsEvent>() {

    private var walletId: String = ""
    private var txId: String = ""
    private var initEventId: String = ""
    private var roomId: String = ""
    private var roomWallet: RoomWallet? = null

    private var remoteSigners: List<SingleSigner> = emptyList()

    private var masterSigners: List<MasterSigner> = emptyList()

    private var joinKeys: List<JoinKey> = emptyList()

    private var contacts: List<Contact> = emptyList()

    override val initialState = TransactionDetailsState()

    fun init(walletId: String, txId: String, initEventId: String, roomId: String) {
        this.walletId = walletId
        this.txId = txId
        this.initEventId = initEventId
        this.roomId = roomId
    }

    private fun getAllSigners() {
        viewModelScope.launch {
            getAllSignersUseCase.execute()
                .flowOn(IO)
                .onException {
                    masterSigners = emptyList()
                    remoteSigners = emptyList()
                }
                .collect {
                    masterSigners = it.first
                    remoteSigners = it.second
                }
        }
    }

    private fun getContacts() {
        viewModelScope.launch {
            getContactsUseCase.execute()
                .defaultSchedulers()
                .subscribe({ contacts = it }, { contacts = emptyList() })
                .addToDisposables()
        }
    }

    private fun getRoomWallet() {
        viewModelScope.launch {
            getRoomWalletUseCase.execute(roomId)
                .flowOn(IO)
                .onException { }
                .collect {
                    joinKeys = it?.joinKeys().orEmpty()
                    roomWallet = it
                    updateJoinKeys()
                }
        }
    }

    private fun updateJoinKeys() {
        val transaction = getState().transaction
        val signers = transaction.signers
        val signedMasterSigners = masterSigners.filter { it.device.masterFingerprint in signers }.map(MasterSigner::toModel)
        val signedRemoteSigners = remoteSigners.filter { it.masterFingerprint in signers }.map(SingleSigner::toModel)
        val localSignedSigners = signedMasterSigners + signedRemoteSigners
        if (joinKeys.isNotEmpty()) {
            updateState { copy(signers = joinKeys.map { it.retrieveInfo(localSignedSigners) }) }
        }
    }

    // why not use name but email?
    private fun JoinKey.retrieveInfo(localSignedSigners: List<SignerModel>): SignerModel {
        return localSignedSigners.firstOrNull { it.fingerPrint == masterFingerprint }
            ?: return copy(name = contacts.firstOrNull { it.chatId == chatId }?.email.orEmpty()).toSignerModel().copy(localKey = false)
    }

    fun getTransactionInfo() {
        getAllSigners()
        if (isSharedTransaction()) {
            getContacts()
            getRoomWallet()
        }
        getTransaction()
    }

    private fun getTransaction() {
        viewModelScope.launch {
            getTransactionUseCase.execute(walletId, txId)
                .onException { event(TransactionDetailsError(it.message.orEmpty())) }
                .collect { onRetrieveTransactionSuccess(it) }
        }
    }

    private fun onRetrieveTransactionSuccess(transaction: Transaction) {
        Timber.tag(TAG).d("transaction::${transaction}")
        updateTransaction(transaction)
    }

    private fun updateTransaction(transaction: Transaction) {
        updateState { copy(transaction = transaction) }
        val signers = transaction.signers
        val signedMasterSigners = masterSigners.filter { it.device.masterFingerprint in signers }.map(MasterSigner::toModel)
        val signedRemoteSigners = remoteSigners.filter { it.masterFingerprint in signers }.map(SingleSigner::toModel)
        val localSignedSigners = signedMasterSigners + signedRemoteSigners
        if (joinKeys.isNotEmpty()) {
            updateState { copy(signers = joinKeys.map { localSignedSigners.firstOrNull { local -> local.fingerPrint == it.masterFingerprint } ?: it.toSignerModel() }) }
        } else {
            updateState { copy(signers = localSignedSigners) }
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
            event(LoadingEvent)
            broadcastTransactionUseCase.execute(walletId, txId)
                .onException { event(TransactionDetailsError(it.message.orEmpty())) }
                .collect {
                    updateTransaction(it)
                    event(BroadcastTransactionSuccess())
                }
        }
    }

    private fun broadcastSharedTransaction() {
        viewModelScope.launch {
            event(LoadingEvent)
            broadcastRoomTransactionUseCase.execute(initEventId)
                .flowOn(IO)
                .onException { event(TransactionDetailsError(it.message.orEmpty())) }
                .collect { event(BroadcastTransactionSuccess(SessionHolder.getActiveRoomId())) }
        }
    }

    fun handleViewBlockchainEvent() {
        getBlockchainExplorerUrlUseCase.execute(txId)
            .flowOn(IO)
            .onException { event(TransactionDetailsError(it.message.orEmpty())) }
            .onEach { event(ViewBlockchainExplorer(it)) }
            .flowOn(Main)
            .launchIn(viewModelScope)
    }

    fun handleMenuMoreEvent() {
        val pending = getState().transaction.status.isPending()
        if (pending) {
            event(PromptTransactionOptions(pending))
        }
    }

    fun handleDeleteTransactionEvent() {
        viewModelScope.launch {
            when (val result = deleteTransactionUseCase.execute(walletId, txId)) {
                is Success -> event(DeleteTransactionSuccess)
                is Error -> event(TransactionDetailsError("${result.exception.message.orEmpty()},walletId::$walletId,txId::$txId"))
            }
        }
    }

    fun handleSignEvent(signer: SignerModel) {
        if (signer.software) {
            viewModelScope.launch {
                val fingerPrint = signer.fingerPrint
                val device = masterSigners.first { it.device.masterFingerprint == fingerPrint }.device
                if (device.needPassPhraseSent) {
                    event(PromptInputPassphrase {
                        viewModelScope.launch {
                            sendSignerPassphrase.execute(signer.id, it)
                                .onException { event(TransactionDetailsError("${it.message.orEmpty()},walletId::$walletId,txId::$txId")) }
                                .collect { signTransaction(device) }
                        }
                    })
                } else {
                    signTransaction(device)
                }
            }
        } else {
            event(PromptTransactionOptions(true))
        }
    }

    private fun signTransaction(device: Device) {
        if (isSharedTransaction()) {
            signRoomTransaction(device)
        } else {
            signPersonalTransaction(device)
        }
    }

    private fun isSharedTransaction() = roomId.isNotEmpty()

    fun exportTransactionToFile() {
        viewModelScope.launch {
            event(LoadingEvent)
            when (val result = createShareFileUseCase.execute("${walletId}_${txId}")) {
                is Success -> exportTransaction(result.data)
                is Error -> {
                    val message = "${result.exception.messageOrUnknownError()},walletId::$walletId,txId::$txId"
                    event(ExportTransactionError(message))
                    CrashlyticsReporter.recordException(TransactionException(message))
                }
            }
        }
    }

    private fun exportTransaction(filePath: String) {
        viewModelScope.launch {
            when (val result = exportTransactionUseCase.execute(walletId, txId, filePath)) {
                is Success -> event(ExportToFileSuccess(filePath))
                is Error -> {
                    val message = "${result.exception.messageOrUnknownError()},walletId::$walletId,txId::$txId"
                    event(ExportTransactionError(message))
                    CrashlyticsReporter.recordException(TransactionException(message))
                }
            }
        }
    }

    private fun signRoomTransaction(device: Device) {
        viewModelScope.launch {
            signRoomTransactionUseCase.execute(initEventId = initEventId, device = device)
                .flowOn(IO)
                .onException {
                    val message = "${it.message.orEmpty()},walletId::$walletId,txId::$txId"
                    event(TransactionDetailsError(message))
                    CrashlyticsReporter.recordException(TransactionException(message))
                }
                .collect { event(SignTransactionSuccess(SessionHolder.getActiveRoomId())) }
        }
    }

    private fun signPersonalTransaction(device: Device) {
        viewModelScope.launch {
            when (val result = signTransactionUseCase.execute(walletId, txId, device)) {
                is Success -> {
                    updateTransaction(result.data)
                    event(SignTransactionSuccess())
                }
                is Error -> {
                    val message = "${result.exception.messageOrUnknownError()},walletId::$walletId,txId::$txId"
                    event(TransactionDetailsError(message))
                    CrashlyticsReporter.recordException(TransactionException(message))
                }
            }
        }
    }

    companion object {
        private const val TAG = "TransactionDetailsViewModel"
    }

}

class TransactionException(message: String) : Exception(message)