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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.RollOverWalletParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.data.model.isInheritanceClaimFlow
import com.nunchuk.android.core.data.model.isOffChainClaim
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
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TxInput
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.defaultRate
import com.nunchuk.android.model.setting.TaprootFeeSelectionSetting
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.AssignTagEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.CreateTxErrorEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.CreateTxSuccessEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.DraftTransactionSuccess
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.InitRoomTransactionError
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.InitRoomTransactionSuccess
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.LoadingEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.UpdateChangeAddress
import com.nunchuk.android.transaction.components.send.receipt.TimelockCoin
import com.nunchuk.android.usecase.CreateTransactionUseCase
import com.nunchuk.android.usecase.DraftSatsCardTransactionUseCase
import com.nunchuk.android.usecase.DraftTransactionUseCase
import com.nunchuk.android.usecase.EstimateFeeForSigningPathsUseCase
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.EstimateRollOverFeeForSigningPathsUseCase
import com.nunchuk.android.usecase.GetTaprootSelectionFeeSettingUseCase
import com.nunchuk.android.usecase.GetTimelockedCoinsUseCase
import com.nunchuk.android.usecase.coin.AddToCoinTagUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.coin.GetCoinsFromTxInputsUseCase
import com.nunchuk.android.usecase.coin.IsMyCoinUseCase
import com.nunchuk.android.usecase.membership.GetSavedAddressListLocalUseCase
import com.nunchuk.android.usecase.room.transaction.InitRoomTransactionUseCase
import com.nunchuk.android.usecase.transaction.GetTransaction2UseCase
import com.nunchuk.android.usecase.transaction.SaveTaprootKeySetSelectionUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val estimateFeeForSigningPathsUseCase: EstimateFeeForSigningPathsUseCase,
    private val getTimelockedCoinsUseCase: GetTimelockedCoinsUseCase,
    private val getCoinsFromTxInputsUseCase: GetCoinsFromTxInputsUseCase,
    private val getTransaction2UseCase: GetTransaction2UseCase,
    private val estimateRollOverFeeForSigningPathsUseCase: EstimateRollOverFeeForSigningPathsUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(TransactionConfirmUiState())
    val uiState = _state.asStateFlow()

    private val _event = MutableSharedFlow<TransactionConfirmEvent>()
    val event = _event.asSharedFlow()

    private var manualFeeRate: Int = -1
    private lateinit var walletId: String
    lateinit var txReceipts: List<TxReceipt>
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
        viewModelScope.launch {
            getTaprootSelectionFeeSettingUseCase(Unit)
                .map { it.getOrThrow() }
                .collect { taprootFeeSelectionSetting ->
                    _state.update { it.copy(feeSelectionSetting = taprootFeeSelectionSetting) }
                }
        }
    }

    fun init(
        walletId: String,
        txReceipts: List<TxReceipt> = emptyList(),
        subtractFeeFromAmount: Boolean = false,
        privateNote: String = "",
        manualFeeRate: Int = -1,
        slots: List<SatsCardSlot> = emptyList(),
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

    /**
     * @param isSendAll indicates whether the transaction is sending all coins include locked coins in the timelock notice screen
     * It doesn't mean all coins in the wallet.
     */
    fun updateInputs(isSendAll: Boolean, inputs: List<UnspentOutput>) {
        this.inputs.clear()
        this.inputs.addAll(inputs)
        if (!isSendAll) {
            if (txReceipts.size == 1) {
                txReceipts = txReceipts.toMutableList().apply {
                    this[0] = this[0].copy(
                        amount = Amount(inputs.sumOf { it.amount.value }).pureBTC()
                    )
                }
                subtractFeeFromAmount = true
            }
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

    fun draftTransaction(
        signingPath: SigningPath?
    ) {
        if (claimInheritanceTxParam.isInheritanceClaimFlow()) {
            draftInheritanceTransaction()
        } else if (slots.isNotEmpty()) {
            draftSatsCardTransaction()
        } else {
            draftNormalTransaction(signingPath)
        }
    }

    private fun draftNormalTransaction(signingPath: SigningPath? = null) {
        viewModelScope.launch {
            _event.emit(LoadingEvent())
            draftNormalTransaction(
                useScriptPath = signingPath != null,
                signingPath = signingPath
            ).onSuccess {
                onDraftTransactionSuccess(it)
            }.onFailure {
                _event.emit(CreateTxErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    private suspend fun draftNormalTransaction(
        useScriptPath: Boolean = false,
        signingPath: SigningPath? = null
    ): Result<Transaction> {
        return draftTransactionUseCase(
            DraftTransactionUseCase.Params(
                walletId = walletId,
                outputs = getOutputs(),
                subtractFeeFromAmount = subtractFeeFromAmount,
                feeRate = manualFeeRate.toManualFeeRate(),
                inputs = inputs.map { TxInput(it.txid, it.vout) },
                useScriptPath = useScriptPath,
                signingPath = signingPath
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
                    bsms = claimInheritanceTxParam?.bsms,
                    antiFeeSniping = antiFeeSniping,
                    subtractFeeFromAmount = subtractFeeFromAmount
                )
            )
            if (result.isSuccess) {
                onDraftTransactionSuccess(result.getOrThrow().transaction)
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

    fun draftMiniscriptTransaction(signingPath: SigningPath? = null) {
        viewModelScope.launch {
            if (savedStateHandle.get<Boolean>(CUSTOMIZE_TRANSACTION_KEY) == true) {
                _event.emit(
                    TransactionConfirmEvent.CustomizeTransaction(
                        signingPath = signingPath
                    )
                )
                return@launch
            }
            _event.emit(LoadingEvent())
            draftNormalTransaction(
                signingPath = signingPath,
                useScriptPath = signingPath != null
            ).onSuccess { transaction ->
                val coins = getCoinsFromTxInputsUseCase(
                    GetCoinsFromTxInputsUseCase.Params(
                        walletId,
                        transaction.inputs
                    )
                ).getOrDefault(emptyList())
                if (coins.isEmpty()) {
                    _event.emit(CreateTxErrorEvent("No coins found for the transaction inputs."))
                    return@launch
                }
                getTimelockedCoinsUseCase(
                    GetTimelockedCoinsUseCase.Params(
                        walletId = walletId,
                        inputs = transaction.inputs
                    )
                ).onSuccess { (maxLockValue, lockedCoins) ->
                    if (lockedCoins.isNotEmpty() && lockedCoins.size < coins.size) {
                        val timelockCoin = TimelockCoin(
                            coins = coins,
                            timelock = maxLockValue,
                            lockedCoins = lockedCoins,
                            signingPath = signingPath
                        )
                        _event.emit(TransactionConfirmEvent.ShowTimeLockNotice(timelockCoin))
                    } else {
                        // No locked coin or all coins are locked
                        handleConfirmEvent(
                            keySetIndex = if (signingPath != null) 1 else 0,
                            signingPath = signingPath
                        )
                    }
                }.onFailure {
                    _event.emit(CreateTxErrorEvent(it.message.orUnknownError()))
                }
            }.onFailure {
                _event.emit(CreateTxErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    fun checkShowTaprootDraftTransaction() {
        viewModelScope.launch {
            runCatching {
                val autoSelectionSetting = uiState.value.feeSelectionSetting
                val draftTxKeyPath = draftNormalTransaction(false).getOrThrow()
                val draftTxScriptPath = draftNormalTransaction(true).getOrThrow()

                val feeDifference = draftTxScriptPath.fee - draftTxKeyPath.fee
                // check fee difference percentage with draftTxKeyPath
                val feeDifferencePercentage =
                    if (draftTxKeyPath.fee.value > 0) {
                        feeDifference.value * 100f / draftTxKeyPath.fee.value
                    } else {
                        100f
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
        keySetIndex: Int = 0,
        signingPath: SigningPath? = null
    ) {
        if (sessionHolder.hasActiveRoom()) {
            initRoomTransaction()
        } else {
            if (isInheritanceClaimingFlow()) {
                createInheritanceTransaction()
            } else {
                createNewTransaction(
                    isQuickCreateTransaction = isQuickCreateTransaction,
                    keySetIndex = keySetIndex,
                    signingPath = signingPath
                )
            }
        }
    }

    fun isInheritanceClaimingFlow() = claimInheritanceTxParam.isInheritanceClaimFlow()

    fun isOffChainClaimingFlow() = claimInheritanceTxParam.isOffChainClaim()

    private fun createNewTransaction(
        isQuickCreateTransaction: Boolean,
        keySetIndex: Int,
        signingPath: SigningPath?
    ) {
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
                    isAssistedWallet = assistedWalletManager.isSyncableWallet(walletId),
                    antiFeeSniping = antiFeeSniping,
                    useScriptPath = useScriptPath,
                    signingPath = signingPath
                )
            ).onSuccess { transaction ->
                saveTaprootKeySetSelectionUseCase(
                    SaveTaprootKeySetSelectionUseCase.Param(
                        transactionId = transaction.txId,
                        keySetIndex = keySetIndex
                    )
                )
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
                bsms = claimInheritanceTxParam?.bsms,
                antiFeeSniping = antiFeeSniping,
                subtractFeeFromAmount = subtractFeeFromAmount
            )
        )
        if (result.isSuccess) {
            _event.emit(
                CreateTxSuccessEvent(
                    result.getOrThrow().transaction,
                    result.getOrThrow().walletId
                )
            )
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

    fun checkMiniscriptSigningPaths() {
        viewModelScope.launch {
            _event.emit(TransactionConfirmEvent.ChooseSigningPathsSuccess)
        }
    }

    fun checkMiniscriptSigningPolicyTransaction(txId: String) {
        viewModelScope.launch {
            getTransaction2UseCase(
                GetTransaction2UseCase.Params(
                    walletId = walletId,
                    txId = txId
                )
            ).onSuccess { transaction ->
                estimateFeeForSigningPathsUseCase(
                    EstimateFeeForSigningPathsUseCase.Params(
                        walletId = walletId,
                        outputs = transaction.outputs.filterIndexed { index, _ -> index != transaction.changeIndex }
                            .associate { it.first to it.second },
                        subtractFeeFromAmount = transaction.subtractFeeFromAmount,
                        feeRate = transaction.feeRate,
                        inputs = transaction.inputs,
                    )
                ).onSuccess { result ->
                    if (result.size > 1) {
                        _event.emit(TransactionConfirmEvent.ChooseSigningPolicy(result))
                    } else if (result.size == 1) {
                        _event.emit(
                            TransactionConfirmEvent.AutoSelectSigningPath(
                                signingPath = result.first().first
                            )
                        )
                    }
                }.onFailure {
                    _event.emit(CreateTxErrorEvent(it.message.orUnknownError()))
                }
            }.onFailure {
                _event.emit(CreateTxErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    fun checkMiniscriptSigningPolicyRollOverTransaction(rollOverWalletParam: RollOverWalletParam) {
        viewModelScope.launch {
            fetchFeeRateIfNeeded()
            estimateRollOverFeeForSigningPathsUseCase(
                EstimateRollOverFeeForSigningPathsUseCase.Params(
                    oldWalletId = walletId,
                    newWalletId = rollOverWalletParam.newWalletId,
                    feeRate = manualFeeRate.toManualFeeRate(),
                    tags = rollOverWalletParam.tags,
                    collections = rollOverWalletParam.collections
                )
            ).onSuccess { result ->
                if (result.size > 1) {
                    _event.emit(TransactionConfirmEvent.ChooseSigningPolicy(result))
                } else if (result.size == 1) {
                    _event.emit(
                        TransactionConfirmEvent.AutoSelectSigningPath(
                            signingPath = result.first().first
                        )
                    )
                }
            }.onFailure {
                _event.emit(CreateTxErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    fun checkMiniscriptSigningPolicy(isSelectPath: Boolean = false) {
        viewModelScope.launch {
            fetchFeeRateIfNeeded()
            estimateFeeForSigningPathsUseCase(
                EstimateFeeForSigningPathsUseCase.Params(
                    walletId = walletId,
                    outputs = getOutputs(),
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    feeRate = manualFeeRate.toManualFeeRate(),
                    inputs = inputs.map { TxInput(it.txid, it.vout) },
                )
            ).onSuccess { result ->
                if (result.size > 1) {
                    _event.emit(TransactionConfirmEvent.ChooseSigningPolicy(result))
                } else if (result.size == 1) {
                    if (isSelectPath) {
                        _event.emit(
                            TransactionConfirmEvent.AutoSelectSigningPath(
                                signingPath = result.first().first
                            )
                        )
                    } else {
                        draftMiniscriptTransaction(
                            signingPath = result.first().first
                        )
                    }
                }
            }.onFailure {
                _event.emit(CreateTxErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    private suspend fun fetchFeeRateIfNeeded() {
        if (manualFeeRate == -1) {
            manualFeeRate = estimateFeeUseCase(Unit).getOrNull()?.defaultRate ?: -1
        }
    }

    fun setCustomizeTransaction(isCustomize: Boolean) {
        savedStateHandle[CUSTOMIZE_TRANSACTION_KEY] = isCustomize
    }

    companion object {
        private const val WAITING_FOR_CONSUME_EVENT_SECONDS = 5L
        private const val CUSTOMIZE_TRANSACTION_KEY = "customize_transaction"
    }
}

data class TransactionConfirmUiState(
    val allTags: Map<Int, CoinTag> = emptyMap(),
    val transaction: Transaction = Transaction(),
    val savedAddress: Map<String, String> = emptyMap(),
    val feeSelectionSetting: TaprootFeeSelectionSetting = TaprootFeeSelectionSetting(),
)

internal fun Int.toManualFeeRate() = if (this > 0) toAmount() else Amount(-1)