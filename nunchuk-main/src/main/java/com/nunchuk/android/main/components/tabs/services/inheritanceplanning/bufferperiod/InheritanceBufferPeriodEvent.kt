package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod

import com.nunchuk.android.model.Period

sealed class InheritanceBufferPeriodEvent {
    data class Loading(val isLoading: Boolean) : InheritanceBufferPeriodEvent()
    data class Error(val message: String) : InheritanceBufferPeriodEvent()
    data class OnContinueClick(val period: Period?) : InheritanceBufferPeriodEvent()
}

data class InheritanceBufferPeriodState(val options: List<BufferPeriodOption> = emptyList())

data class BufferPeriodOption(val period: Period, val isSelected: Boolean)
