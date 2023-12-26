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

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.data.model.isInheritanceClaimFlow
import com.nunchuk.android.core.domain.membership.InheritanceClaimCreateTransactionUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.sum
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
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
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EstimatedFeeViewModel @Inject constructor(
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val draftTransactionUseCase: DraftTransactionUseCase,
    private val draftSatsCardTransactionUseCase: DraftSatsCardTransactionUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val inheritanceClaimCreateTransactionUseCase: InheritanceClaimCreateTransactionUseCase
) : NunchukViewModel<EstimatedFeeState, EstimatedFeeEvent>() {

    private var walletId: String = ""
    private var txReceipts: List<TxReceipt> = emptyList()
    private var forceSubtractFeeFromAmount: Boolean = false
    private var draftTranJob: Job? = null
    private val slots = mutableListOf<SatsCardSlot>()
    private val inputs = mutableListOf<UnspentOutput>()
    private var address = ""
    private var claimInheritanceTxParam: ClaimInheritanceTxParam? = null
    override val initialState = EstimatedFeeState()

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
        getEstimateFeeRates()
        if (slots.isEmpty()) {
            getAllTags()
            getAllCoins()
        }
    }

    fun updateNewInputs(inputs: List<UnspentOutput>) {
        this.inputs.apply {
            clear()
            addAll(inputs)
        }
        getEstimateFeeRates()
    }

    private fun getAllTags() {
        viewModelScope.launch {
            getAllTagsUseCase(walletId).onSuccess {
                updateState { copy(allTags = it.associateBy { it.id }) }
            }
        }
    }

    private fun getAllCoins() {
        viewModelScope.launch {
            getAllCoinUseCase(walletId).onSuccess {
                updateState { copy(allCoins = it) }
            }
        }
    }

    fun getEstimateFeeRates() {
        viewModelScope.launch {
            val result = estimateFeeUseCase(Unit)
            if (result.isSuccess) {
                setEvent(EstimatedFeeEvent.GetFeeRateSuccess(result.getOrThrow()))
                updateState { copy(estimateFeeRates = result.getOrThrow(), manualFeeRate = result.getOrThrow().defaultRate) }
            } else {
                setEvent(EstimatedFeeErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
                updateState { copy(estimateFeeRates = EstimateFeeRates()) }
            }
            if (walletId.isNotEmpty()) {
                draftTransaction()
            }
        }
    }

    private fun draftTransaction() {
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
        setEvent(EstimatedFeeEvent.Loading(true))
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
        when (val result = draftTransactionUseCase.execute(
            walletId = walletId,
            outputs = getOutputs(),
            subtractFeeFromAmount = subtractFeeFromAmount,
            feeRate = state.manualFeeRate.toManualFeeRate(),
            inputs = inputs.map { TxInput(it.txid, it.vout) }
        )) {
            is Success -> {
                updateState {
                    copy(
                        estimatedFee = result.data.fee,
                        inputs = result.data.inputs,
                        cpfpFee = result.data.cpfpFee,
                        subtractFeeFromAmount = subtractFeeFromAmount,
                        enableSubtractFeeFromAmount = enableSubtractFeeFromAmount,
                    )
                }
                setEvent(EstimatedFeeEvent.DraftTransactionSuccess)
            }
            is Error -> {
                if (result.exception !is CancellationException) {
                    setEvent(EstimatedFeeErrorEvent(result.exception.message.orEmpty()))
                }
            }
        }
        setEvent(EstimatedFeeEvent.Loading(false))
    }

    fun getOutputAmount() = txReceipts.sumOf { it.amount }

    private fun getOutputs(): Map<String, Amount> {
        val outputs = mutableMapOf<String, Amount>()
        txReceipts.forEach {
            outputs[it.address] = it.amount.toAmount()
        }
        return outputs
    }


    private suspend fun draftSatsCardTransaction() {
        setEvent(EstimatedFeeEvent.Loading(true))
        val result = draftSatsCardTransactionUseCase(
            DraftSatsCardTransactionUseCase.Data(
                txReceipts.first().address,
                slots,
                getState().manualFeeRate
            )
        )
        setEvent(EstimatedFeeEvent.Loading(false))
        if (result.isSuccess) {
            updateState { copy(estimatedFee = result.getOrThrow().fee) }
        } else {
            if (result.exceptionOrNull() !is CancellationException) {
                setEvent(EstimatedFeeErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private suspend fun draftInheritanceTransaction() {
        setEvent(EstimatedFeeEvent.Loading(true))
        val result = inheritanceClaimCreateTransactionUseCase(
            InheritanceClaimCreateTransactionUseCase.Param(
                masterSignerIds = claimInheritanceTxParam?.masterSignerIds.orEmpty(),
                address = address,
                magic = claimInheritanceTxParam?.magicalPhrase.orEmpty(),
                feeRate = getState().manualFeeRate.toManualFeeRate(),
                derivationPaths = claimInheritanceTxParam?.derivationPaths.orEmpty(),
                isDraft = true
            )
        )
        setEvent(EstimatedFeeEvent.Loading(false))
        if (result.isSuccess) {
            updateState { copy(estimatedFee = result.getOrThrow().fee) }
        } else {
            if (result.exceptionOrNull() !is CancellationException) {
                setEvent(EstimatedFeeErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun handleSubtractFeeSwitch(checked: Boolean, enable : Boolean = true) {
        updateState { copy(subtractFeeFromAmount = checked, enableSubtractFeeFromAmount = enable) }
        draftTransaction()
    }

    fun handleManualFeeSwitch(checked: Boolean) {
        updateState { copy(manualFeeDetails = checked) }
        updateFeeRate(defaultRate)
    }

    fun handleContinueEvent() {
        getState().apply {
            event(
                EstimatedFeeCompletedEvent(
                    estimatedFee = estimatedFee.pureBTC(),
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    manualFeeRate = manualFeeRate
                )
            )
        }
    }

    fun updateFeeRate(feeRate: Int) {
        if (feeRate != getState().manualFeeRate) {
            updateState { copy(manualFeeRate = feeRate) }
            draftTransaction()
        }
    }

    fun validateFeeRate(feeRate: Int): Boolean {
        if (feeRate < getState().estimateFeeRates.minimumFee) {
            setEvent(EstimatedFeeEvent.InvalidManualFee)
            return false
        }
        return true
    }

    val defaultRate: Int
        get() = getState().estimateFeeRates.defaultRate

    fun getSelectedCoins() : List<UnspentOutput> = inputs

    fun getInputsCoins() : List<UnspentOutput> {
        val inputs = getState().inputs
        return getState().allCoins.filter { coin -> inputs.any { input -> input.first == coin.txid && input.second == coin.vout } }
    }
}