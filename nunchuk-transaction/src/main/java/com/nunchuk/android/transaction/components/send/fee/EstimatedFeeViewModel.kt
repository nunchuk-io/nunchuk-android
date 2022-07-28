package com.nunchuk.android.transaction.components.send.fee

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.transaction.components.send.confirmation.toManualFeeRate
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent.EstimatedFeeCompletedEvent
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent.EstimatedFeeErrorEvent
import com.nunchuk.android.usecase.DraftTransactionUseCase
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class EstimatedFeeViewModel @Inject constructor(
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val draftTransactionUseCase: DraftTransactionUseCase
) : NunchukViewModel<EstimatedFeeState, EstimatedFeeEvent>() {

    private var walletId: String = ""
    private var address: String = ""
    private var sendAmount: Double = 0.0
    override val initialState = EstimatedFeeState()

    fun init(walletId: String, address: String, sendAmount: Double) {
        this.walletId = walletId
        this.address = address
        this.sendAmount = sendAmount
        getEstimateFeeRates()
    }

    private fun getEstimateFeeRates() {
        viewModelScope.launch {
            estimateFeeUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { updateState { copy(estimateFeeRates = EstimateFeeRates()) } }
                .flowOn(Dispatchers.Main)
                .collect { updateState { copy(estimateFeeRates = it, manualFeeRate = it.standardRate) } }
        }
    }

    private fun draftTransaction() {
        val state = getState()
        viewModelScope.launch {
            setEvent(EstimatedFeeEvent.Loading(true))
            when (val result = draftTransactionUseCase.execute(
                walletId = walletId,
                outputs = mapOf(address to sendAmount.toAmount()),
                subtractFeeFromAmount = state.subtractFeeFromAmount,
                feeRate = state.manualFeeRate.toManualFeeRate()
            )) {
                is Success -> updateState { copy(estimatedFee = result.data.fee) }
                is Error -> event(EstimatedFeeErrorEvent(result.exception.message.orEmpty()))
            }
            setEvent(EstimatedFeeEvent.Loading(false))
        }
    }

    fun handleCustomizeFeeSwitch(checked: Boolean, isSendingAll: Boolean) {
        if (checked) {
            updateState { copy(customizeFeeDetails = true) }
        } else {
            updateState {
                copy(
                    customizeFeeDetails = false,
                    manualFeeDetails = false,
                    manualFeeRate = defaultRate,
                    subtractFeeFromAmount = isSendingAll
                )
            }
        }
    }

    fun handleSubtractFeeSwitch(checked: Boolean) {
        updateState { copy(subtractFeeFromAmount = checked) }
        draftTransaction()
    }

    fun handleManualFeeSwitch(checked: Boolean) {
        updateState { copy(manualFeeDetails = checked, manualFeeRate = defaultRate) }
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
        val newFeeRate = feeRate.coerceAtLeast(getState().estimateFeeRates.economicRate)
        if (newFeeRate != getState().manualFeeRate) {
            updateState { copy(manualFeeRate = newFeeRate) }
            draftTransaction()
        }
    }

    fun validateFeeRate(feeRate: Int): Boolean {
        if (feeRate < getState().estimateFeeRates.economicRate) {
            setEvent(EstimatedFeeEvent.InvalidManualFee)
            return false
        }
        return true
    }

    val defaultRate: Int
        get() = getState().estimateFeeRates.standardRate
}