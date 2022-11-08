package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownperiod

sealed class LockdownPeriodEvent {
    data class Loading(val isLoading: Boolean) : LockdownPeriodEvent()
    object ContinueClick : LockdownPeriodEvent()
}

data class LockdownPeriodState(
    val options: List<PeriodOption> = emptyList()
)

data class PeriodOption(val title: String, val isSelected: Boolean)
