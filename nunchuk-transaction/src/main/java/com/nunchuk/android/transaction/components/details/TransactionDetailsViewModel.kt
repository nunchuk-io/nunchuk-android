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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
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

    private fun getContacts() {
        viewModelScope.launch {
            getContactsUseCase.execute()
                .defaultSchedulers()
                .subscribe({ contacts = it }, { contacts = emptyList() })
                .addToDisposables()
        }
    }

    fun getTransactionInfo() {
        if (isSharedTransaction()) {
            getSharedTransaction()
        } else {
            getPersonalTransaction()
        }
    }

    private fun getPersonalTransaction() {
        viewModelScope.launch {
            getAllSignersUseCase.execute()
                .zip(getTransactionUseCase.execute(walletId, txId)
                    .onException { event(TransactionDetailsError(it.message.orEmpty())) }
                ) { p, tx -> Triple(p.first, p.second, tx) }
                .flowOn(IO)
                .collect {
                    masterSigners = it.first
                    remoteSigners = it.second
                    updateTransaction(it.third)
                }
        }
    }

    private fun getSharedTransaction() {
        getContacts()
        viewModelScope.launch {
            getAllSignersUseCase.execute()
                .zip(getRoomWalletUseCase.execute(roomId).map {
                    joinKeys = it?.joinKeys().orEmpty()
                    roomWallet = it
                }
                ) { p, _ -> p }
                .zip(getTransactionUseCase.execute(walletId, txId)
                    .onException { event(TransactionDetailsError(it.message.orEmpty())) }
                ) { p, tx -> Triple(p.first, p.second, tx) }
                .flowOn(IO)
                .collect {
                    masterSigners = it.first
                    remoteSigners = it.second
                    updateTransaction(it.third)
                }
        }
    }

    private fun updateTransaction(transaction: Transaction) {
        updateState { copy(transaction = transaction) }
        val signers = transaction.signers
        val signedMasterSigners = masterSigners.filter { it.device.masterFingerprint in signers }.map(MasterSigner::toModel)
        val signedRemoteSigners = remoteSigners.filter { it.masterFingerprint in signers }.map(SingleSigner::toModel)
        val localSignedSigners = signedMasterSigners + signedRemoteSigners
        if (joinKeys.isNotEmpty()) {
            updateState { copy(signers = joinKeys.map { it.retrieveInfo(localSignedSigners, contacts) }) }
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
                                .flowOn(IO)
                                .onException { event(TransactionDetailsError("${it.message.orEmpty()},walletId::$walletId,txId::$txId")) }
                                .collect { signTransaction(device, signer.id) }
                        }
                    })
                } else {
                    signTransaction(device, signer.id)
                }
            }
        } else {
            event(PromptTransactionOptions(true))
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

    private fun signRoomTransaction(device: Device, signerId: String) {
        viewModelScope.launch {
            signRoomTransactionUseCase.execute(initEventId = initEventId, device = device, signerId)
                .flowOn(IO)
                .onException {
                    val message = "${it.message.orEmpty()},walletId::$walletId,txId::$txId"
                    event(TransactionDetailsError(message))
                    CrashlyticsReporter.recordException(TransactionException(message))
                }
                .collect { event(SignTransactionSuccess(SessionHolder.getActiveRoomId())) }
        }
    }

    private fun signPersonalTransaction(device: Device, signerId: String) {
        viewModelScope.launch {
            when (val result = signTransactionUseCase.execute(walletId, txId, device, signerId)) {
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

}

class TransactionException(message: String) : Exception(message)

// why not use name but email?
internal fun JoinKey.retrieveInfo(localSignedSigners: List<SignerModel>, contacts: List<Contact>) = retrieveByLocalKeys(localSignedSigners) ?: retrieveByContacts(contacts)

internal fun JoinKey.retrieveByLocalKeys(localSignedSigners: List<SignerModel>) = localSignedSigners.firstOrNull { it.fingerPrint == masterFingerprint }

internal fun JoinKey.retrieveByContacts(contacts: List<Contact>) = copy(name = getDisplayName(contacts)).toSignerModel().copy(localKey = false)

internal fun JoinKey.getDisplayName(contacts: List<Contact>): String {
    contacts.firstOrNull { it.chatId == chatId }?.apply {
        if (email.isNotEmpty()) {
            return@getDisplayName email
        }
        if (name.isNotEmpty()) {
            return@getDisplayName chatId
        }
    }
    return chatId
}