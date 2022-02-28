package com.nunchuk.android.transaction.components.details

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetBlockchainExplorerUrlUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isPending
import com.nunchuk.android.model.Device
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.*
import com.nunchuk.android.usecase.*
import com.nunchuk.android.usecase.room.transaction.BroadcastRoomTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.SignRoomTransactionUseCase
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
) : NunchukViewModel<TransactionDetailsState, TransactionDetailsEvent>() {

    private lateinit var walletId: String
    private lateinit var txId: String
    private lateinit var initEventId: String
    private var remoteSigners: List<SingleSigner> = emptyList()
    private var masterSigners: List<MasterSigner> = emptyList()

    override val initialState = TransactionDetailsState()

    fun init(walletId: String, txId: String, initEventId: String) {
        this.walletId = walletId
        this.txId = txId
        this.initEventId = initEventId
    }

    fun getTransactionInfo() {
        viewModelScope.launch {
            getAllSignersUseCase.execute()
                .flowOn(IO)
                .catch {
                    masterSigners = emptyList()
                    remoteSigners = emptyList()
                }
                .collect {
                    masterSigners = it.first
                    remoteSigners = it.second
                }
            getTransactionUseCase.execute(walletId, txId)
                .onException { event(TransactionDetailsError(it.message.orEmpty())) }
                .collect { onRetrieveTransactionSuccess(it) }
        }
    }

    private fun onRetrieveTransactionSuccess(transaction: Transaction) {
        updateTransaction(transaction)
    }

    private fun updateTransaction(transaction: Transaction) {
        updateState { copy(transaction = transaction) }
        val signers = transaction.signers
        if (signers.isNotEmpty()) {
            val signedMasterSigners = masterSigners.filter { it.device.masterFingerprint in signers }.map(MasterSigner::toModel)
            val signedRemoteSigners = remoteSigners.filter { it.masterFingerprint in signers }.map(SingleSigner::toModel)
            updateState { copy(signers = signedMasterSigners + signedRemoteSigners) }
        }
    }

    fun handleViewMoreEvent() {
        updateState { copy(viewMore = !viewMore) }
    }

    fun handleBroadcastEvent() {
        if (SessionHolder.hasActiveRoom()) {
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
        event(PromptTransactionOptions(getState().transaction.status.isPending()))
    }

    fun handleDeleteTransactionEvent() {
        viewModelScope.launch {
            when (val result = deleteTransactionUseCase.execute(walletId, txId)) {
                is Success -> event(DeleteTransactionSuccess)
                is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
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
                                .onException { event(TransactionDetailsError(it.message.orEmpty())) }
                                .collect { signTransaction(device) }
                        }
                    })
                } else {
                    signTransaction(device)
                }
            }
        } else {
            event(PromptTransactionOptions(false))
        }
    }

    private fun signTransaction(device: Device) {
        if (SessionHolder.hasActiveRoom()) {
            signRoomTransaction(device)
        } else {
            signPersonalTransaction(device)
        }
    }

    private fun signRoomTransaction(device: Device) {
        viewModelScope.launch {
            signRoomTransactionUseCase.execute(initEventId = initEventId, device = device)
                .flowOn(IO)
                .onException { event(TransactionDetailsError(it.message.orEmpty())) }
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
                is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
            }
        }
    }

}