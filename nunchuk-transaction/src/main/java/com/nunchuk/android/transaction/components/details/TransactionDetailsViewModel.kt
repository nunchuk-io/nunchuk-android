package com.nunchuk.android.transaction.components.details

import android.nfc.tech.IsoDep
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.SignRoomTransactionByTapSignerUseCase
import com.nunchuk.android.core.domain.SignTransactionByTapSignerUseCase
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
    private val getContactsUseCase: GetContactsUseCase,
    private val signTransactionByTapSignerUseCase: SignTransactionByTapSignerUseCase,
    private val signRoomTransactionByTapSignerUseCase: SignRoomTransactionByTapSignerUseCase
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
        getContactsUseCase.execute()
            .defaultSchedulers()
            .subscribe({
                contacts = it
                getSharedTransaction()
            }, { contacts = emptyList() })
            .addToDisposables()
    }

    fun getTransactionInfo() {
        setEvent(LoadingEvent)
        if (isSharedTransaction()) {
            getContacts()
        } else {
            getPersonalTransaction()
        }
    }

    private fun getPersonalTransaction() {
        viewModelScope.launch {
            getAllSignersUseCase.execute()
                .zip(getTransactionUseCase.execute(walletId, txId)) { p, tx -> Triple(p.first, p.second, tx) }
                .flowOn(IO)
                .onException { setEvent(TransactionDetailsError(it.message.orEmpty())) }
                .collect {
                    masterSigners = it.first
                    remoteSigners = it.second
                    updateTransaction(it.third)
                }
        }
    }

    private fun getSharedTransaction() {
        viewModelScope.launch {
            getAllSignersUseCase.execute()
                .zip(getRoomWalletUseCase.execute(roomId).map {
                    joinKeys = it?.joinKeys().orEmpty()
                    roomWallet = it
                }
                ) { p, _ -> p }
                .zip(getTransactionUseCase.execute(walletId, txId)) { p, tx -> Triple(p.first, p.second, tx) }
                .flowOn(IO)
                .onException { setEvent(TransactionDetailsError(it.message.orEmpty())) }
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
        val signedMasterSigners = masterSigners.filter { it.device.masterFingerprint in signers }
            .map(MasterSigner::toModel)
        val signedRemoteSigners =
            remoteSigners.filter { it.masterFingerprint in signers }.map(SingleSigner::toModel)
        val localSignedSigners = signedMasterSigners + signedRemoteSigners
        if (joinKeys.isNotEmpty()) {
            updateState {
                copy(signers = joinKeys.map {
                    it.retrieveInfo(
                        localSignedSigners,
                        contacts
                    )
                })
            }
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
            setEvent(LoadingEvent)
            broadcastTransactionUseCase.execute(walletId, txId)
                .flowOn(IO)
                .onException { setEvent(TransactionDetailsError(it.message.orEmpty())) }
                .collect {
                    updateTransaction(it)
                    setEvent(BroadcastTransactionSuccess())
                }
        }
    }

    private fun broadcastSharedTransaction() {
        viewModelScope.launch {
            setEvent(LoadingEvent)
            broadcastRoomTransactionUseCase.execute(initEventId)
                .flowOn(IO)
                .onException { setEvent(TransactionDetailsError(it.message.orEmpty())) }
                .collect { setEvent(BroadcastTransactionSuccess(SessionHolder.getActiveRoomId())) }
        }
    }

    fun handleViewBlockchainEvent() {
        getBlockchainExplorerUrlUseCase.execute(txId)
            .flowOn(IO)
            .onException { setEvent(TransactionDetailsError(it.message.orEmpty())) }
            .onEach { setEvent(ViewBlockchainExplorer(it)) }
            .flowOn(Main)
            .launchIn(viewModelScope)
    }

    fun handleMenuMoreEvent() {
        val pending = getState().transaction.status.isPending()
        if (pending) {
            setEvent(PromptTransactionOptions(pending))
        }
    }

    fun handleDeleteTransactionEvent() {
        viewModelScope.launch {
            when (val result = deleteTransactionUseCase.execute(walletId, txId)) {
                is Success -> setEvent(DeleteTransactionSuccess)
                is Error -> setEvent(TransactionDetailsError("${result.exception.message.orEmpty()},walletId::$walletId,txId::$txId"))
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
                            sendSignerPassphrase.execute(signer.id, it)
                                .flowOn(IO)
                                .onException { setEvent(TransactionDetailsError("${it.message.orEmpty()},walletId::$walletId,txId::$txId")) }
                                .collect { signTransaction(device, signer.id) }
                        }
                    })
                } else {
                    signTransaction(device, signer.id)
                }
            }
        } else {
            setEvent(PromptTransactionOptions(true))
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
            when (val result = createShareFileUseCase.execute("${walletId}_${txId}")) {
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
                .flowOn(IO)
                .onException {
                    fireSignError(it)
                }
                .collect { setEvent(SignTransactionSuccess(SessionHolder.getActiveRoomId())) }
        }
    }

    private fun signPersonalTransaction(device: Device, signerId: String) {
        viewModelScope.launch {
            when (val result = signTransactionUseCase.execute(walletId, txId, device, signerId)) {
                is Success -> {
                    updateTransaction(result.data)
                    setEvent(SignTransactionSuccess())
                }
                is Error -> {
                    fireSignError(result.exception)
                }
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

    private fun signRoomTransactionTapSigner(isoDep: IsoDep, inputCvc: String) {
        viewModelScope.launch {
            setEvent(NfcLoadingEvent)
            val result = signRoomTransactionByTapSignerUseCase(SignRoomTransactionByTapSignerUseCase.Data(isoDep, inputCvc, initEventId))
            if (result.isSuccess) {
                setEvent(SignTransactionSuccess(SessionHolder.getActiveRoomId()))
            } else {
                fireSignError(result.exceptionOrNull())
            }
        }
    }

    private fun signPersonTapSignerTransaction(isoDep: IsoDep, inputCvc: String) {
        viewModelScope.launch {
            setEvent(NfcLoadingEvent)
            val result = signTransactionByTapSignerUseCase(
                SignTransactionByTapSignerUseCase.Data(
                    isoDep = isoDep,
                    cvc = inputCvc,
                    walletId = walletId,
                    txId = txId
                )
            )
            if (result.isSuccess) {
                updateTransaction(result.getOrThrow())
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
}

class TransactionException(message: String) : Exception(message)

// why not use name but email?
internal fun JoinKey.retrieveInfo(localSignedSigners: List<SignerModel>, contacts: List<Contact>) =
    retrieveByLocalKeys(localSignedSigners) ?: retrieveByContacts(contacts)

internal fun JoinKey.retrieveByLocalKeys(localSignedSigners: List<SignerModel>) =
    localSignedSigners.firstOrNull { it.fingerPrint == masterFingerprint }

internal fun JoinKey.retrieveByContacts(contacts: List<Contact>) =
    copy(name = getDisplayName(contacts)).toSignerModel().copy(localKey = false)

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