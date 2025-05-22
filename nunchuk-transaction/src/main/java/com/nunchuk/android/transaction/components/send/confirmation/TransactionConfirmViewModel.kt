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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.data.model.isInheritanceClaimFlow
import com.nunchuk.android.core.domain.membership.InheritanceClaimCreateTransactionUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.fromBTCToCurrency
import com.nunchuk.android.core.util.hasChangeIndex
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
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
import com.nunchuk.android.usecase.CreateTransactionUseCase
import com.nunchuk.android.usecase.DraftSatsCardTransactionUseCase
import com.nunchuk.android.usecase.DraftTransactionUseCase
import com.nunchuk.android.usecase.GetTaprootSelectionFeeSettingUseCase
import com.nunchuk.android.usecase.coin.AddToCoinTagUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.coin.IsMyCoinUseCase
import com.nunchuk.android.usecase.membership.GetSavedAddressListLocalUseCase
import com.nunchuk.android.usecase.room.transaction.InitRoomTransactionUseCase
import com.nunchuk.android.usecase.transaction.SaveTaprootKeySetSelectionUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
    private val addToCoinTagUseCase: AddToCoinTagUseCase,
    private val getTaprootSelectionFeeSettingUseCase: GetTaprootSelectionFeeSettingUseCase,
    private val saveTaprootKeySetSelectionUseCase: SaveTaprootKeySetSelectionUseCase,
    private val getSavedAddressListLocalUseCase: GetSavedAddressListLocalUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(TransactionConfirmUiState())
    val uiState = _state.asStateFlow()

    private val _event = MutableSharedFlow<TransactionConfirmEvent>()
    val event = _event.asSharedFlow()

    private var manualFeeRate: Int = -1
    private lateinit var walletId: String
    private lateinit var txReceipts: List<TxReceipt>
    private var subtractFeeFromAmount: Boolean = false
    private val slots = mutableListOf<SatsCardSlot>()
    private val inputs = mutableListOf<UnspentOutput>()
    private lateinit var privateNote: String
    private var claimInheritanceTxParam: ClaimInheritanceTxParam? = null
    private var antiFeeSniping: Boolean = false

    init {
        viewModelScope.launch {
            getSavedAddressListLocalUseCase(Unit)
                .map { it.getOrThrow() }
                .collect { savedAddresses ->
                    _state.update { it.copy(savedAddress = savedAddresses.associate { it.address to it.label }) }
                }
        }
    }

    fun init(
        walletId: String,
        txReceipts: List<TxReceipt>,
        subtractFeeFromAmount: Boolean,
        privateNote: String,
        manualFeeRate: Int,
        slots: List<SatsCardSlot>,
        inputs: List<UnspentOutput> = emptyList(),
        claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
        antiFeeSniping: Boolean = false,
    ) {
        this.walletId = walletId
        this.txReceipts = txReceipts
        this.subtractFeeFromAmount = subtractFeeFromAmount
        this.privateNote = privateNote
        this.manualFeeRate = manualFeeRate
        this.antiFeeSniping = antiFeeSniping
        this.slots.apply {
            clear()
            addAll(slots)
        }
        this.inputs.apply {
            clear()
            addAll(inputs)
        }
        this.claimInheritanceTxParam = claimInheritanceTxParam
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
        viewModelScope.launch {
            _event.emit(LoadingEvent())
            val roomId = sessionHolder.getActiveRoomId()
            initRoomTransactionUseCase.execute(
                roomId = roomId,
                outputs = getOutputs(),
                subtractFeeFromAmount = subtractFeeFromAmount,
                feeRate = manualFeeRate.toManualFeeRate()
            )
                .flowOn(Dispatchers.IO)
                .onException { _event.emit(InitRoomTransactionError(it.message.orUnknownError())) }
                .collect {
                    delay(WAITING_FOR_CONSUME_EVENT_SECONDS)
                    _event.emit(InitRoomTransactionSuccess(roomId))
                }
        }
    }

    fun draftTransaction() {
        if (claimInheritanceTxParam.isInheritanceClaimFlow()) {
            draftInheritanceTransaction()
        } else if (slots.isNotEmpty()) {
            draftSatsCardTransaction()
        } else {
            draftNormalTransaction()
        }
    }

    private fun draftNormalTransaction() {
        viewModelScope.launch {
            _event.emit(LoadingEvent())
            draftNormalTransaction(false).onSuccess {
                onDraftTransactionSuccess(it)
            }.onFailure {
                _event.emit(CreateTxErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    private suspend fun draftNormalTransaction(useScriptPath: Boolean = false): Result<Transaction> {
        return draftTransactionUseCase(
            DraftTransactionUseCase.Params(
                walletId = walletId,
                outputs = getOutputs(),
                subtractFeeFromAmount = subtractFeeFromAmount,
                feeRate = manualFeeRate.toManualFeeRate(),
                inputs = inputs.map { TxInput(it.txid, it.vout) },
                useScriptPath = useScriptPath
            )
        )
    }

    private fun draftSatsCardTransaction() {
        viewModelScope.launch {
            _event.emit(LoadingEvent())
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
                _event.emit(CreateTxErrorEvent(result.exceptionOrNull()?.message.orEmpty()))
            }
        }
    }

    private fun draftInheritanceTransaction() {
        viewModelScope.launch {
            _event.emit(LoadingEvent())
            val result = inheritanceClaimCreateTransactionUseCase(
                InheritanceClaimCreateTransactionUseCase.Param(
                    masterSignerIds = claimInheritanceTxParam?.masterSignerIds.orEmpty(),
                    address = txReceipts.first().address,
                    magic = claimInheritanceTxParam?.magicalPhrase.orEmpty(),
                    feeRate = manualFeeRate.toManualFeeRate(),
                    derivationPaths = claimInheritanceTxParam?.derivationPaths.orEmpty(),
                    isDraft = true,
                    amount = claimInheritanceTxParam?.customAmount ?: 0.0,
                    antiFeeSniping = antiFeeSniping
                )
            )
            if (result.isSuccess) {
                onDraftTransactionSuccess(result.getOrThrow())
            } else {
                _event.emit(CreateTxErrorEvent(result.exceptionOrNull()?.message.orEmpty()))
            }
        }
    }

    fun isMyCoin(output: TxOutput) =
        runBlocking { isMyCoinUseCase(IsMyCoinUseCase.Param(walletId, output.first)) }.getOrDefault(
            false
        )

    private suspend fun onDraftTransactionSuccess(data: Transaction) {
        _state.update { state ->
            state.copy(transaction = data)
        }
        _event.emit(DraftTransactionSuccess(data))
        val hasChange: Boolean = data.hasChangeIndex()
        if (hasChange) {
            val txOutput = data.outputs[data.changeIndex]
            _event.emit(UpdateChangeAddress(txOutput.first, txOutput.second))
        } else {
            _event.emit(UpdateChangeAddress("", Amount(0)))
        }
    }

    fun checkShowTaprootDraftTransaction() {
        viewModelScope.launch {
            runCatching {
                val autoSelectionSetting =
                    getTaprootSelectionFeeSettingUseCase(Unit).map { it.getOrThrow() }.first()
                val draftTxKeyPath = draftNormalTransaction(false).getOrThrow()
                val draftTxScriptPath = draftNormalTransaction(true).getOrThrow()

                val feeDifference = draftTxScriptPath.fee - draftTxKeyPath.fee
                // check fee difference percentage with draftTxKeyPath
                val feeDifferencePercentage =
                    if (draftTxKeyPath.fee.value > 0) {
                        feeDifference.value / draftTxKeyPath.fee.value
                    } else {
                        100
                    }
                val draftTx =
                    if (!autoSelectionSetting.automaticFeeEnabled || feeDifference.pureBTC()
                            .fromBTCToCurrency() > autoSelectionSetting.feeDifferenceThresholdCurrency
                        || feeDifferencePercentage > autoSelectionSetting.feeDifferenceThresholdPercent
                    ) {
                        TaprootDraftTransaction(
                            draftTxKeyPath = draftTxKeyPath,
                            draftTxScriptPath = draftTxScriptPath,
                        )
                    } else null
                _event.emit(
                    TransactionConfirmEvent.DraftTaprootTransactionSuccess(draftTx)
                )
            }.onFailure {
                _event.emit(
                    CreateTxErrorEvent(it.message.orUnknownError())
                )
            }
        }
    }

    /**
     * @param keySetIndex only for taproot transaction, 0 is value keyset, other is script path
     */
    fun handleConfirmEvent(
        isQuickCreateTransaction: Boolean = false,
        keySetIndex: Int = 0
    ) {
        if (sessionHolder.hasActiveRoom()) {
            initRoomTransaction()
        } else {
            if (isInheritanceClaimingFlow()) {
                createInheritanceTransaction()
            } else {
                createNewTransaction(
                    isQuickCreateTransaction = isQuickCreateTransaction,
                    keySetIndex = keySetIndex
                )
            }
        }
    }

    fun isInheritanceClaimingFlow() = claimInheritanceTxParam.isInheritanceClaimFlow()

    private fun createNewTransaction(isQuickCreateTransaction: Boolean, keySetIndex: Int) {
        viewModelScope.launch {
            _event.emit(LoadingEvent())
            val useScriptPath = keySetIndex > 0
            createTransactionUseCase(
                CreateTransactionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    outputs = getOutputs(),
                    inputs = inputs.map { TxInput(it.txid, it.vout) },
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    feeRate = manualFeeRate.toManualFeeRate(),
                    memo = privateNote,
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId),
                    antiFeeSniping = antiFeeSniping,
                    useScriptPath = useScriptPath,
                )
            ).onSuccess { transaction ->
                if (keySetIndex > 0) {
                    saveTaprootKeySetSelectionUseCase(
                        SaveTaprootKeySetSelectionUseCase.Param(
                            transactionId = transaction.txId,
                            keySetIndex = keySetIndex
                        )
                    )
                }
                val commonTagMap = mutableMapOf<Int, Int>()
                inputs.forEach { output ->
                    output.tags.forEach {
                        commonTagMap[it] = commonTagMap.getOrDefault(it, 0).inc()
                    }
                }
                val commonTags = commonTagMap.map { it.key }
                if (commonTags.isNotEmpty() && transaction.hasChangeIndex()) {
                    val tags = commonTags.mapNotNull { tagId -> _state.value.allTags[tagId] }
                    val output = UnspentOutput(
                        txid = transaction.txId,
                        vout = transaction.changeIndex
                    )
                    if (isQuickCreateTransaction) {
                        addAddTagsToChangeCoin(tags, output, transaction.txId)
                    } else {
                        _event.emit(
                            AssignTagEvent(
                                walletId = walletId,
                                txId = transaction.txId,
                                output = output,
                                tags = tags
                            )
                        )
                    }
                } else {
                    _event.emit(CreateTxSuccessEvent(transaction))
                }
                pushEventManager.push(PushEvent.TransactionCreatedEvent)
            }.onFailure { exception ->
                _event.emit(CreateTxErrorEvent(exception.message.orUnknownError()))
            }
        }
    }

    private fun createInheritanceTransaction() = viewModelScope.launch {
        _event.emit(LoadingEvent(isClaimInheritance = true))
        val result = inheritanceClaimCreateTransactionUseCase(
            InheritanceClaimCreateTransactionUseCase.Param(
                address = txReceipts.first().address,
                feeRate = manualFeeRate.toManualFeeRate(),
                masterSignerIds = claimInheritanceTxParam?.masterSignerIds.orEmpty(),
                magic = claimInheritanceTxParam?.magicalPhrase.orEmpty(),
                derivationPaths = claimInheritanceTxParam?.derivationPaths.orEmpty(),
                isDraft = false,
                amount = claimInheritanceTxParam?.customAmount ?: 0.0,
                antiFeeSniping = antiFeeSniping
            )
        )
        if (result.isSuccess) {
            _event.emit(CreateTxSuccessEvent(result.getOrThrow()))
        } else {
            _event.emit(CreateTxErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    private fun addAddTagsToChangeCoin(tags: List<CoinTag>, output: UnspentOutput, txId: String) =
        viewModelScope.launch {
            addToCoinTagUseCase(
                AddToCoinTagUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    tagIds = tags.map { it.id },
                    coins = listOf(output),
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
                )
            ).onSuccess {
                _event.emit(TransactionConfirmEvent.AssignTagSuccess(txId))
            }.onFailure {
                _event.emit(TransactionConfirmEvent.AssignTagError(it.message.orUnknownError()))
            }
        }

    fun getSavedAddress() = _state.value.savedAddress

    companion object {
        private const val WAITING_FOR_CONSUME_EVENT_SECONDS = 5L
    }
}

data class TransactionConfirmUiState(
    val allTags: Map<Int, CoinTag> = emptyMap(),
    val transaction: Transaction = Transaction(),
    val savedAddress: Map<String, String> = emptyMap(),
)

internal fun Int.toManualFeeRate() = if (this > 0) toAmount() else Amount(-1)