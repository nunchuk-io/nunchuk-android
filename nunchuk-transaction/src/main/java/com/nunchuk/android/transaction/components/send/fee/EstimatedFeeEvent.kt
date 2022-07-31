package com.nunchuk.android.transaction.components.send.fee

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.EstimateFeeRates

sealed class EstimatedFeeEvent {
    class Loading(val isLoading: Boolean) : EstimatedFeeEvent()
    object InvalidManualFee : EstimatedFeeEvent()
    data class EstimatedFeeErrorEvent(val message: String) : EstimatedFeeEvent()
    data class EstimatedFeeCompletedEvent(
        val estimatedFee: Double,
        val subtractFeeFromAmount: Boolean,
        val manualFeeRate: Int
    ) : EstimatedFeeEvent()
}

data class EstimatedFeeState(
    val estimatedFee: Amount = Amount.ZER0,
    val customizeFeeDetails: Boolean = false,
    val subtractFeeFromAmount: Boolean = false,
    val manualFeeDetails: Boolean = false,
    val estimateFeeRates: EstimateFeeRates = EstimateFeeRates(),
    val manualFeeRate: Int = estimateFeeRates.defaultRate
)