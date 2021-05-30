package com.nunchuk.android.transaction.details

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.details.TransactionDetailsEvent.*
import com.nunchuk.android.usecase.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class TransactionDetailsViewModel @Inject constructor(
    private val getBlockchainExplorerUrlUseCase: GetBlockchainExplorerUrlUseCase,
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val getRemoteSignersUseCase: GetRemoteSignersUseCase,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val signTransactionUseCase: SignTransactionUseCase,
    private val broadcastTransactionUseCase: BroadcastTransactionUseCase
) : NunchukViewModel<TransactionDetailsState, TransactionDetailsEvent>() {

    private lateinit var walletId: String
    private lateinit var txId: String
    private var remoteSigners: List<SingleSigner> = emptyList()
    private var masterSigners: List<MasterSigner> = emptyList()

    override val initialState = TransactionDetailsState()

    fun init(walletId: String, txId: String) {
        this.walletId = walletId
        this.txId = txId
        getTransactionInfo()
    }

    private fun getTransactionInfo() {
        viewModelScope.launch {
            masterSigners = when (val result = getMasterSignersUseCase.execute()) {
                is Success -> result.data
                is Error -> emptyList()
            }

            remoteSigners = when (val result = getRemoteSignersUseCase.execute()) {
                is Success -> result.data
                is Error -> emptyList()
            }

            when (val result = getTransactionUseCase.execute(walletId, txId)) {
                is Success -> onRetrieveTransactionSuccess(result.data)
                is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
            }
        }
    }

    private fun onRetrieveTransactionSuccess(transaction: Transaction) {
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
        viewModelScope.launch {
            when (val result = broadcastTransactionUseCase.execute(walletId, txId)) {
                is Success -> {
                    updateState { copy(transaction = result.data) }
                    event(BroadcastTransactionSuccess)
                }
                is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
            }
        }
    }

    fun handleViewBlockchainEvent() {
        viewModelScope.launch {
            when (val result = getBlockchainExplorerUrlUseCase.execute(txId)) {
                is Success -> event(ViewBlockchainExplorer(result.data))
                is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
            }
        }
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
            val fingerPrint = signer.fingerPrint
            viewModelScope.launch {
                when (val result = signTransactionUseCase.execute(walletId, txId, masterSigners.first { it.device.masterFingerprint == fingerPrint }.device)) {
                    is Success -> {
                        updateState { copy(transaction = result.data) }
                        event(SignTransactionSuccess)
                    }
                    is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
                }
            }
        } else {
            // FIXME
        }
    }

}