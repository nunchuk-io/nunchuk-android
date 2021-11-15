package com.nunchuk.android.transaction.components.send.fee

import com.nunchuk.android.model.Amount

sealed class EstimatedFeeEvent {
    data class EstimatedFeeErrorEvent(val message: String) : EstimatedFeeEvent()
    data class EstimatedFeeCompletedEvent(
        val estimatedFee: Double,
        val subtractFeeFromAmount: Boolean,
        val manualFeeRate: Int
    ) : EstimatedFeeEvent()

    object Loading : EstimatedFeeEvent()
}

data class EstimatedFeeState(
    val estimatedFee: Amount = Amount.ZER0,
    val customizeFeeDetails: Boolean = false,
    val subtractFeeFromAmount: Boolean = false,
    val manualFeeDetails: Boolean = false,
    val manualFeeRate: Int = 0
)