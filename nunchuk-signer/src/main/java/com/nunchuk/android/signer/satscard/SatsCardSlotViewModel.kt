package com.nunchuk.android.signer.satscard

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetSatsCardSlotBalanceUseCase
import com.nunchuk.android.core.domain.GetSatsCardSlotKeyUseCase
import com.nunchuk.android.core.domain.SweepSatsCardSlotUseCase
import com.nunchuk.android.core.domain.UnsealSatsCardSlotUseCase
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.type.SatsCardSlotStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SatsCardSlotViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getSatsCardSlotBalanceUseCase: GetSatsCardSlotBalanceUseCase,
    private val unsealSatsCardSlotUseCase: UnsealSatsCardSlotUseCase,
    private val sweepSatsCardSlotUseCase: SweepSatsCardSlotUseCase,
    private val getSatsCardSlotKeyUseCase: GetSatsCardSlotKeyUseCase
) : ViewModel() {
    private val _state = savedStateHandle.getStateFlow(SatsCardArgs.EXTRA_SATSCARD_STATUS, SatsCardStatus())
    private val _event = MutableStateFlow<SatsCardSlotEvent?>(null)
    val event = _event.asStateFlow().filterIsInstance<SatsCardSlotEvent>()

    init {
        val status: SatsCardStatus = savedStateHandle[SatsCardArgs.EXTRA_SATSCARD_STATUS]!!
        viewModelScope.launch {
            _event.value = SatsCardSlotEvent.Loading
            val activeSlot = status.slots.find { it.index == status.activeSlotIndex } ?: return@launch
            val result = getSatsCardSlotBalanceUseCase(listOf(activeSlot))
            if (result.isSuccess) {
                val previousStatus = _state.value
                val newSlot = result.getOrThrow().first()
                val newSlots = previousStatus.slots.toMutableList().apply {
                    set(status.activeSlotIndex, newSlot)
                }
                savedStateHandle[SatsCardArgs.EXTRA_SATSCARD_STATUS] = previousStatus.copy(slots = newSlots)
                _event.value = SatsCardSlotEvent.GetActiveSlotBalanceSuccess(newSlot)
            } else {
                _event.value = SatsCardSlotEvent.ShowError(result.exceptionOrNull())
            }
        }
        viewModelScope.launch {
            val otherSlots = status.slots.filter { it.index != status.activeSlotIndex }
            val result = getSatsCardSlotBalanceUseCase(otherSlots)
            if (result.isSuccess) {
                val previousStatus = _state.value
                val activeSlot = status.slots.find { it.index == status.activeSlotIndex } ?: return@launch
                savedStateHandle[SatsCardArgs.EXTRA_SATSCARD_STATUS] = previousStatus.copy(slots = result.getOrThrow() + activeSlot)
                _event.value = SatsCardSlotEvent.GetOtherSlotBalanceSuccess(result.getOrThrow())
            } else {
                _event.value = SatsCardSlotEvent.ShowError(result.exceptionOrNull())
            }
        }
    }

    fun unsealSweepActiveSlot(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            val result = unsealSatsCardSlotUseCase(UnsealSatsCardSlotUseCase.Data(isoDep, cvc))
            if (result.isSuccess) {
                sweepUnsealSlots(listOf(result.getOrThrow()))
            }
        }
    }

    fun getSlotsKey(isoDep: IsoDep, cvc: String) {
        val unsealSlots = _state.value.slots.filter { it.status == SatsCardSlotStatus.SEALED && it.balance.value > 0L }
        viewModelScope.launch {
            val result = getSatsCardSlotKeyUseCase(GetSatsCardSlotKeyUseCase.Data(isoDep, cvc, unsealSlots))
            if (result.isSuccess) {
                sweepUnsealSlots(result.getOrThrow())
            }
        }
    }

    private fun sweepUnsealSlots(slots: List<SatsCardSlot>) {
        // TODO Hai
    }

    fun getUnsealSlots() = _state.value.slots.filter { it.status == SatsCardSlotStatus.UNSEALED }
}

sealed class SatsCardSlotEvent {
    class GetActiveSlotBalanceSuccess(val slot: SatsCardSlot) : SatsCardSlotEvent()
    class GetOtherSlotBalanceSuccess(val slots: List<SatsCardSlot>) : SatsCardSlotEvent()
    class ShowError(val e: Throwable?) : SatsCardSlotEvent()
    object Loading : SatsCardSlotEvent()
}