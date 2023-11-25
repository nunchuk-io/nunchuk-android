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

package com.nunchuk.android.transaction.components.send.confirmation

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.domain.membership.InheritanceClaimCreateTransactionUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.hasChangeIndex
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TxInput
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.AssignTagEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.CreateTxErrorEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.CreateTxSuccessEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.DraftTransactionSuccess
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.InitRoomTransactionError
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.InitRoomTransactionSuccess
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.LoadingEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.UpdateChangeAddress
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent
import com.nunchuk.android.usecase.CreateTransactionUseCase
import com.nunchuk.android.usecase.DraftSatsCardTransactionUseCase
import com.nunchuk.android.usecase.DraftTransactionUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.coin.IsMyCoinUseCase
import com.nunchuk.android.usecase.room.transaction.InitRoomTransactionUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class TransactionConfirmViewModel @Inject constructor(
    private val draftTransactionUseCase: DraftTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val initRoomTransactionUseCase: InitRoomTransactionUseCase,
    private val draftSatsCardTransactionUseCase: DraftSatsCardTransactionUseCase,
    private val sessionHolder: SessionHolder,
    private val assistedWalletManager: AssistedWalletManager,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val inheritanceClaimCreateTransactionUseCase: InheritanceClaimCreateTransactionUseCase,
    private val pushEventManager: PushEventManager,
    private val isMyCoinUseCase: IsMyCoinUseCase,
    ) : NunchukViewModel<Unit, TransactionConfirmEvent>() {
    private val _state = MutableStateFlow(TransactionConfirmUiState())
    val uiState = _state.asStateFlow()

    private var manualFeeRate: Int = -1
    private lateinit var walletId: String
    private lateinit var txReceipts: List<TxReceipt>
    private var subtractFeeFromAmount: Boolean = false
    private val slots = mutableListOf<SatsCardSlot>()
    private val inputs = mutableListOf<UnspentOutput>()
    private lateinit var privateNote: String
    private var masterSignerId: String = ""
    private var magicalPhrase: String = ""
    private var derivationPath: String = ""
    private var isInheritanceFlow = false

    override val initialState = Unit

    fun init(
        walletId: String,
        txReceipts: List<TxReceipt>,
        subtractFeeFromAmount: Boolean,
        privateNote: String,
        manualFeeRate: Int,
        slots: List<SatsCardSlot>,
        masterSignerId: String,
        magicalPhrase: String,
        inputs: List<UnspentOutput> = emptyList(),
        derivationPath: String
    ) {
        this.walletId = walletId
        this.txReceipts = txReceipts
        this.subtractFeeFromAmount = subtractFeeFromAmount
        this.privateNote = privateNote
        this.manualFeeRate = manualFeeRate
        this.slots.apply {
            clear()
            addAll(slots)
        }
        this.inputs.apply {
            clear()
            addAll(inputs)
        }
        this.masterSignerId = masterSignerId
        this.magicalPhrase = magicalPhrase
        this.derivationPath = derivationPath
        isInheritanceFlow = magicalPhrase.isNotEmpty() && masterSignerId.isNotEmpty()
        if (inputs.isNotEmpty()) {
            getAllTags()
        }
    }

    private fun getOutputs(): Map<String, Amount> {
        val outputs = mutableMapOf<String, Amount>()
        txReceipts.forEach {
            outputs[it.address] = it.amount.toAmount()
        }
        return outputs
    }

    private fun getAllTags() {
        viewModelScope.launch {
            getAllTagsUseCase(walletId).onSuccess { tags ->
                _state.update { it.copy(allTags = tags.associateBy { tag -> tag.id }) }
            }
        }
    }

    private fun initRoomTransaction() {
        event(LoadingEvent())
        viewModelScope.launch {
            val roomId = sessionHolder.getActiveRoomId()
            initRoomTransactionUseCase.execute(
                roomId = roomId,
                outputs = getOutputs(),
                subtractFeeFromAmount = subtractFeeFromAmount,
                feeRate = manualFeeRate.toManualFeeRate()
            )
                .flowOn(Dispatchers.IO)
                .onException { event(InitRoomTransactionError(it.message.orUnknownError())) }
                .collect {
                    delay(WAITING_FOR_CONSUME_EVENT_SECONDS)
                    event(InitRoomTransactionSuccess(roomId))
                }
        }
    }

    fun draftTransaction() {
        if (isInheritanceFlow) {
            draftInheritanceTransaction()
        } else if (slots.isNotEmpty()) {
            draftSatsCardTransaction()
        } else {
            draftNormalTransaction()
        }
    }

    private fun draftNormalTransaction() {
        event(LoadingEvent())
        viewModelScope.launch {
            when (val result = draftTransactionUseCase.execute(
                walletId = walletId,
                outputs = getOutputs(),
                subtractFeeFromAmount = subtractFeeFromAmount,
                feeRate = manualFeeRate.toManualFeeRate(),
                inputs = inputs.map { TxInput(it.txid, it.vout) }
            )) {
                is Success -> onDraftTransactionSuccess(result.data)
                is Error -> event(CreateTxErrorEvent(result.exception.message.orEmpty()))
            }
        }
    }

    private fun draftSatsCardTransaction() {
        event(LoadingEvent())
        viewModelScope.launch {
            val result = draftSatsCardTransactionUseCase(
                DraftSatsCardTransactionUseCase.Data(
                    txReceipts.first().address,
                    slots,
                    manualFeeRate
                )
            )
            if (result.isSuccess) {
                onDraftTransactionSuccess(result.getOrThrow())
            } else {
                event(CreateTxErrorEvent(result.exceptionOrNull()?.message.orEmpty()))
            }
        }
    }

    private fun draftInheritanceTransaction() {
        event(LoadingEvent())
        viewModelScope.launch {
           val result = inheritanceClaimCreateTransactionUseCase(
                InheritanceClaimCreateTransactionUseCase.Param(
                    masterSignerId = masterSignerId,
                    address = txReceipts.first().address,
                    magic = magicalPhrase,
                    feeRate = manualFeeRate.toManualFeeRate(),
                    derivationPath = derivationPath,
                    isDraft = true
                )
            )
            if (result.isSuccess) {
                onDraftTransactionSuccess(result.getOrThrow())
            } else {
                event(CreateTxErrorEvent(result.exceptionOrNull()?.message.orEmpty()))
            }
        }
    }

    fun isMyCoin(output: TxOutput) =
        runBlocking { isMyCoinUseCase(IsMyCoinUseCase.Param(walletId, output.first)) }.getOrDefault(
            false
        )

    private fun onDraftTransactionSuccess(data: Transaction) {
        setEvent(DraftTransactionSuccess(data))
        val hasChange: Boolean = data.hasChangeIndex()
        if (hasChange) {
            val txOutput = data.outputs[data.changeIndex]
            event(UpdateChangeAddress(txOutput.first, txOutput.second))
        } else {
            event(UpdateChangeAddress("", Amount(0)))
        }
    }

    fun handleConfirmEvent(isQuickCreateTransaction: Boolean = false) {
        if (sessionHolder.hasActiveRoom()) {
            initRoomTransaction()
        } else {
            if (isInheritanceClaimingFlow()) {
                createInheritanceTransaction()
            } else {
                createNewTransaction(isQuickCreateTransaction)
            }
        }
    }

    fun isInheritanceClaimingFlow() = masterSignerId.isNotBlank() && magicalPhrase.isNotBlank()

    private fun createNewTransaction(isQuickCreateTransaction: Boolean) {
        viewModelScope.launch {
            event(LoadingEvent())
            val result = createTransactionUseCase(
                CreateTransactionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    outputs = getOutputs(),
                    inputs = inputs.map { TxInput(it.txid, it.vout) },
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    feeRate = manualFeeRate.toManualFeeRate(),
                    memo = privateNote,
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId),
                )
            )
            if (result.isSuccess) {
                val transaction = result.getOrThrow()
                val commonTagMap = mutableMapOf<Int, Int>()
                inputs.forEach { output ->
                    output.tags.forEach {
                        commonTagMap[it] = commonTagMap.getOrDefault(it, 0).inc()
                    }
                }
                val commonTags = commonTagMap.filter { it.value == inputs.size }.map { it.key }
                if (commonTags.isNotEmpty() && transaction.hasChangeIndex() && !isQuickCreateTransaction) {
                    val tags = commonTags.mapNotNull { tagId -> _state.value.allTags[tagId] }
                    setEvent(
                        AssignTagEvent(
                            walletId = walletId,
                            txId =  transaction.txId,
                            output = UnspentOutput(txid = transaction.txId, vout = transaction.changeIndex),
                            tags = tags
                        )
                    )
                } else {
                    setEvent(CreateTxSuccessEvent(result.getOrThrow()))
                }
                pushEventManager.push(PushEvent.TransactionCreatedEvent)
            } else {
                event(CreateTxErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun createInheritanceTransaction() = viewModelScope.launch {
        event(LoadingEvent(isClaimInheritance = true))
        val result = inheritanceClaimCreateTransactionUseCase(
            InheritanceClaimCreateTransactionUseCase.Param(
                address = txReceipts.first().address,
                feeRate = manualFeeRate.toManualFeeRate(),
                masterSignerId = masterSignerId,
                magic = magicalPhrase,
                derivationPath = derivationPath,
                isDraft = false
            )
        )
        if (result.isSuccess) {
            setEvent(CreateTxSuccessEvent(result.getOrThrow()))
        } else {
            event(CreateTxErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    companion object {
        private const val WAITING_FOR_CONSUME_EVENT_SECONDS = 5L
    }
}

data class TransactionConfirmUiState(val allTags: Map<Int, CoinTag> = emptyMap())

internal fun Int.toManualFeeRate() = if (this > 0) toAmount() else Amount(-1)