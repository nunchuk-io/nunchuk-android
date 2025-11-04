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

package com.nunchuk.android.transaction.components.send.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.compose.miniscript.analyzeMiniscriptForTimelocks
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.RollOverWalletParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.data.model.isInheritanceClaimFlow
import com.nunchuk.android.core.domain.membership.InheritanceClaimCreateTransactionUseCase
import com.nunchuk.android.core.util.isValueKeySetDisable
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.sum
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.TxInput
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.defaultRate
import com.nunchuk.android.transaction.components.send.confirmation.toManualFeeRate
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent.EstimatedFeeCompletedEvent
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent.EstimatedFeeErrorEvent
import com.nunchuk.android.usecase.DraftSatsCardTransactionUseCase
import com.nunchuk.android.usecase.DraftTransactionUseCase
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.EstimateRollOverAmountUseCase
import com.nunchuk.android.usecase.GetDefaultAntiFeeSnipingUseCase
import com.nunchuk.android.usecase.GetScriptNodeFromMiniscriptTemplateUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EstimatedFeeViewModel @Inject constructor(
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val draftTransactionUseCase: DraftTransactionUseCase,
    private val draftSatsCardTransactionUseCase: DraftSatsCardTransactionUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val inheritanceClaimCreateTransactionUseCase: InheritanceClaimCreateTransactionUseCase,
    private val estimateRollOverAmountUseCase: EstimateRollOverAmountUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getDefaultAntiFeeSnipingUseCase: GetDefaultAntiFeeSnipingUseCase,
    private val getScriptNodeFromMiniscriptTemplateUseCase: GetScriptNodeFromMiniscriptTemplateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EstimatedFeeState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<EstimatedFeeEvent>()
    val event = _event.asSharedFlow()

    private var walletId: String = ""
    private var txReceipts: List<TxReceipt> = emptyList()
    private var forceSubtractFeeFromAmount: Boolean = false
    private var draftTranJob: Job? = null
    private val slots = mutableListOf<SatsCardSlot>()
    private val inputs = mutableListOf<UnspentOutput>()
    private var address = ""
    private var claimInheritanceTxParam: ClaimInheritanceTxParam? = null
    private var rollOverWalletParam: RollOverWalletParam? = null
    
    fun getState() = _state.value

    fun init(args: EstimatedFeeArgs) {
        this.walletId = args.walletId
        this.txReceipts = args.txReceipts
        this.slots.apply {
            clear()
            addAll(args.slots)
        }
        this.inputs.apply {
            clear()
            addAll(args.inputs)
        }
        address = args.txReceipts.first().address
        forceSubtractFeeFromAmount = args.subtractFeeFromAmount
        claimInheritanceTxParam = args.claimInheritanceTxParam
        rollOverWalletParam = args.rollOverWalletParam
        if (rollOverWalletParam != null) {
            getEstimateRollOverAmount()
        } else {
            getEstimateFeeRates()
        }
        if (slots.isEmpty()) {
            getAllTags()
            getAllCoins()
        }
        getWalletDetail(walletId)
        viewModelScope.launch {
            getDefaultAntiFeeSnipingUseCase(Unit)
                .collect { result ->
                    _state.update { it.copy(antiFeeSniping = result.getOrDefault(false)) }
                    _state.update { it.copy(antiFeeSniping = result.getOrDefault(false)) }
                }
        }
    }

    fun updateNewInputs(inputs: List<UnspentOutput>) {
        this.inputs.apply {
            clear()
            addAll(inputs)
        }
        getEstimateFeeRates()
    }

    private fun getWalletDetail(walletId: String) {
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                _state.update { it.copy(isValueKeySetDisable = wallet.isValueKeySetDisable) }
                if (wallet.miniscript.isNotEmpty()) {
                    getScriptNodeFromMiniscriptTemplateUseCase(wallet.miniscript).onSuccess { result ->
                        val warningInfo = analyzeMiniscriptForTimelocks(result.scriptNode)
                        _state.update { state ->
                            state.copy(timelockInfo = warningInfo)
                        }
                    }
                }
            }
        }
    }

    private fun getAllTags() {
        viewModelScope.launch {
            getAllTagsUseCase(walletId).onSuccess { tags ->
                _state.update { it.copy(allTags = tags.associateBy { it.id }) }
            }
        }
    }

    private fun getAllCoins() {
        viewModelScope.launch {
            getAllCoinUseCase(walletId).onSuccess { coins ->
                _state.update { it.copy(allCoins = coins) }
            }
        }
    }

    private fun getEstimateRollOverAmount() = viewModelScope.launch {
        val resultFeeRate = estimateFeeUseCase(Unit)
        if (resultFeeRate.isSuccess) {
            _state.update {
                it.copy(
                    estimateFeeRates = resultFeeRate.getOrThrow(),
                    manualFeeRate = resultFeeRate.getOrThrow().defaultRate
                )
            }
            val result = estimateRollOverAmountUseCase(
                EstimateRollOverAmountUseCase.Param(
                    oldWalletId = walletId,
                    newWalletId = rollOverWalletParam?.newWalletId.orEmpty(),
                    tags = rollOverWalletParam?.tags.orEmpty(),
                    collections = rollOverWalletParam?.collections.orEmpty(),
                    feeRate = getState().manualFeeRate.toManualFeeRate()
                )
            )
            if (result.isSuccess) {
            _state.update {
                it.copy(
                        estimatedFee = result.getOrThrow().second,
                        manualFeeRate = result.getOrThrow().second.value.toInt(),
                        rollOverWalletPairAmount = result.getOrThrow()
                    )
                }
            } else {
                if (result.exceptionOrNull() !is CancellationException) {
                    _event.emit(EstimatedFeeErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
                }
            }
        }
    }

    fun getEstimateFeeRates(isDraft: Boolean = true) {
        viewModelScope.launch {
            val result = estimateFeeUseCase(Unit)
            if (result.isSuccess) {
                _event.emit(EstimatedFeeEvent.GetFeeRateSuccess(result.getOrThrow()))
                _state.update {
                    it.copy(
                        estimateFeeRates = result.getOrThrow(),
                        manualFeeRate = result.getOrThrow().defaultRate
                    )
                }
            } else {
                _event.emit(EstimatedFeeErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
                _state.update { it.copy(estimateFeeRates = EstimateFeeRates()) }
            }
            if (walletId.isNotEmpty() && isDraft) {
                draftTransaction()
            }
        }
    }

    private fun draftTransaction() {
        if (rollOverWalletParam != null) return
        draftTranJob?.cancel()
        draftTranJob = viewModelScope.launch {
            if (claimInheritanceTxParam.isInheritanceClaimFlow()) {
                draftInheritanceTransaction()
            } else if (slots.isNotEmpty()) {
                draftSatsCardTransaction()
            } else {
                draftNormalTransaction()
            }
        }
    }

    private suspend fun draftNormalTransaction() {
        val state = getState()
        _event.emit(EstimatedFeeEvent.Loading(true))
        // if selected coin amount is smaller than send amount + fee, we should auto check subtract fee and disable toggle
        var subtractFeeFromAmount = state.subtractFeeFromAmount
        var enableSubtractFeeFromAmount = state.enableSubtractFeeFromAmount
        if (!forceSubtractFeeFromAmount && inputs.isNotEmpty()) {
            val selectedAmount = inputs.map { it.amount }.sum()
            if (selectedAmount.value <= state.estimatedFee.value + getOutputAmount().toAmount().value) {
                subtractFeeFromAmount = true
                enableSubtractFeeFromAmount = false
            }
        }

        draftTransactionUseCase(
            DraftTransactionUseCase.Params(
                walletId = walletId,
                outputs = getOutputs(),
                subtractFeeFromAmount = subtractFeeFromAmount,
                feeRate = state.manualFeeRate.toManualFeeRate(),
                inputs = inputs.map { TxInput(it.txid, it.vout) }
            )
        ).onSuccess { tx ->
            _state.update {
                it.copy(
                    estimatedFee = tx.fee,
                    inputs = tx.inputs,
                    cpfpFee = tx.cpfpFee,
                    scriptPathFee = tx.scriptPathFee,
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    enableSubtractFeeFromAmount = enableSubtractFeeFromAmount,
                )
            }
            _event.emit(EstimatedFeeEvent.DraftTransactionSuccess)
        }.onFailure { exception ->
            if (exception !is CancellationException) {
                _event.emit(EstimatedFeeErrorEvent(exception.message.orUnknownError()))
            }
        }
        _event.emit(EstimatedFeeEvent.Loading(false))
    }

    fun getOutputAmount() = txReceipts.sumOf { it.amount }

    fun getRollOverTotalAmount(): Amount {
        return getState().rollOverWalletPairAmount.first + getState().rollOverWalletPairAmount.second
    }

    private fun getOutputs(): Map<String, Amount> {
        val outputs = mutableMapOf<String, Amount>()
        txReceipts.forEach {
            outputs[it.address] = it.amount.toAmount()
        }
        return outputs
    }


    private suspend fun draftSatsCardTransaction() {
        _event.emit(EstimatedFeeEvent.Loading(true))
        val result = draftSatsCardTransactionUseCase(
            DraftSatsCardTransactionUseCase.Data(
                txReceipts.first().address,
                slots,
                getState().manualFeeRate
            )
        )
        _event.emit(EstimatedFeeEvent.Loading(false))
        if (result.isSuccess) {
            _state.update { it.copy(estimatedFee = result.getOrThrow().fee) }
        } else {
            if (result.exceptionOrNull() !is CancellationException) {
                _event.emit(EstimatedFeeErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private suspend fun draftInheritanceTransaction() {
        _event.emit(EstimatedFeeEvent.Loading(true))
        val result = inheritanceClaimCreateTransactionUseCase(
            InheritanceClaimCreateTransactionUseCase.Param(
                masterSignerIds = claimInheritanceTxParam?.masterSignerIds.orEmpty(),
                address = address,
                magic = claimInheritanceTxParam?.magicalPhrase.orEmpty(),
                feeRate = getState().manualFeeRate.toManualFeeRate(),
                derivationPaths = claimInheritanceTxParam?.derivationPaths.orEmpty(),
                isDraft = true,
                amount = claimInheritanceTxParam?.customAmount ?: 0.0,
                bsms = claimInheritanceTxParam?.bsms,
                antiFeeSniping = getAntiFeeSniping()
            )
        )
        _event.emit(EstimatedFeeEvent.Loading(false))
        if (result.isSuccess) {
            _state.update { it.copy(estimatedFee = result.getOrThrow().fee) }
        } else {
            if (result.exceptionOrNull() !is CancellationException) {
                _event.emit(EstimatedFeeErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun handleSubtractFeeSwitch(checked: Boolean, enable: Boolean = true) {
        _state.update { it.copy(subtractFeeFromAmount = checked, enableSubtractFeeFromAmount = enable) }
        draftTransaction()
    }

    fun handleManualFeeSwitch(checked: Boolean) {
        _state.update { it.copy(manualFeeDetails = checked) }
        updateFeeRate(defaultRate)
    }

    fun handleContinueEvent() = viewModelScope.launch {
        getState().apply {
            _event.emit(
                EstimatedFeeCompletedEvent(
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    manualFeeRate = manualFeeRate
                )
            )
        }
    }

    fun updateFeeRate(feeRate: Int) {
        if (feeRate != getState().manualFeeRate) {
            _state.update { it.copy(manualFeeRate = feeRate) }
            draftTransaction()
        }
    }

    fun validateFeeRate(feeRate: Int): Boolean {
        if (feeRate < getState().estimateFeeRates.minimumFee) {
            viewModelScope.launch {
                _event.emit(EstimatedFeeEvent.InvalidManualFee)
            }
            return false
        }
        return true
    }

    fun setAntiFeeSniping(checked: Boolean) {
        _state.update { it.copy(antiFeeSniping = checked) }
    }

    fun getAntiFeeSniping(): Boolean {
        return getState().antiFeeSniping
    }

    val defaultRate: Int
        get() = getState().estimateFeeRates.defaultRate

    fun getSelectedCoins(): List<UnspentOutput> =
        inputs.ifEmpty { getInputsCoins() }

    fun getInputsCoins(): List<UnspentOutput> {
        val inputs = getState().inputs
        return getState().allCoins.filter { coin -> inputs.any { input -> input.first == coin.txid && input.second == coin.vout } }
    }
}