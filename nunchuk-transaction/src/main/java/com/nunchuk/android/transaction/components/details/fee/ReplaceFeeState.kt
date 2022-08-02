package com.nunchuk.android.transaction.components.details.fee

import com.nunchuk.android.model.EstimateFeeRates

data class ReplaceFeeState(
    val estimateFeeRates: EstimateFeeRates = EstimateFeeRates(),
    val manualFeeRate: Int = estimateFeeRates.standardRate
)

sealed class ReplaceFeeEvent {
    class Loading(val isLoading: Boolean) : ReplaceFeeEvent()
    object ReplaceTransactionSuccess : ReplaceFeeEvent()
    class ShowError(val e: Throwable?) : ReplaceFeeEvent()
}