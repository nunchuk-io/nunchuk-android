package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownperiod

import com.nunchuk.android.model.LockdownPeriod

sealed class LockdownPeriodEvent {
    data class Loading(val isLoading: Boolean) : LockdownPeriodEvent()
    data class ProcessFailure(val message: String) : LockdownPeriodEvent()
    data class CalculateRequiredSignaturesSuccess(
        val type: String,
        val walletId: String,
        val userData: String,
        val requiredSignatures: Int
    ) : LockdownPeriodEvent()

    data class LockdownUpdateSuccess(val period: String) : LockdownPeriodEvent()
}

data class LockdownPeriodState(
    val options: List<PeriodOption> = emptyList(),
    val userData: String? = null,
    val period: LockdownPeriod? = null
)

data class PeriodOption(val period: LockdownPeriod, val isSelected: Boolean)
