package com.nunchuk.android.signer.satscard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetSatsCardSlotBalanceUseCase
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.type.SatsCardSlotStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SatsCardSlotViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getSatsCardSlotBalanceUseCase: GetSatsCardSlotBalanceUseCase,
) : ViewModel() {
    private val _event = Channel<SatsCardSlotEvent>(Channel.CONFLATED)
    private val _state = savedStateHandle.getStateFlow(SatsCardArgs.EXTRA_SATSCARD_STATUS, SatsCardStatus())
    private val _activeSlot = MutableStateFlow<SatsCardSlot?>(null)
    val activeSlot = _activeSlot.filterIsInstance<SatsCardSlot>()
    val event = _event.receiveAsFlow()

    init {
        val status: SatsCardStatus = savedStateHandle[SatsCardArgs.EXTRA_SATSCARD_STATUS]!!
        viewModelScope.launch {
            _event.send(SatsCardSlotEvent.Loading)
            val activeSlot = status.slots.find { it.index == status.activeSlotIndex } ?: return@launch
            val result = getSatsCardSlotBalanceUseCase(listOf(activeSlot))
            if (result.isSuccess) {
                val previousStatus = _state.value
                val newSlot = result.getOrThrow().first()
                val newSlots = previousStatus.slots.toMutableList().apply {
                    set(status.activeSlotIndex, newSlot)
                }
                savedStateHandle[SatsCardArgs.EXTRA_SATSCARD_STATUS] = previousStatus.copy(slots = newSlots)
                _activeSlot.value = newSlot
                _event.send(SatsCardSlotEvent.GetActiveSlotBalanceSuccess(newSlot))
            } else {
                _event.send(SatsCardSlotEvent.ShowError(result.exceptionOrNull()))
            }
        }
        viewModelScope.launch {
            val otherSlots = status.slots.filter { it.index != status.activeSlotIndex }
            val result = getSatsCardSlotBalanceUseCase(otherSlots)
            if (result.isSuccess) {
                val previousStatus = _state.value
                val activeSlot = status.slots.find { it.index == status.activeSlotIndex } ?: return@launch
                savedStateHandle[SatsCardArgs.EXTRA_SATSCARD_STATUS] = previousStatus.copy(slots = result.getOrThrow() + activeSlot)
                _event.send(SatsCardSlotEvent.GetOtherSlotBalanceSuccess(result.getOrThrow()))
            } else {
                _event.send(SatsCardSlotEvent.ShowError(result.exceptionOrNull()))
            }
        }
    }

    fun isBalanceLoaded() = _activeSlot.value != null

    fun getUnsealSlots() = _state.value.slots.filter { it.status == SatsCardSlotStatus.UNSEALED }

    fun getActiveSlot(): SatsCardSlot? {
        val status = _state.value
        return status.slots.find { it.index == status.activeSlotIndex }
    }
}

sealed class SatsCardSlotEvent {
    class GetActiveSlotBalanceSuccess(val slot: SatsCardSlot) : SatsCardSlotEvent()
    class GetOtherSlotBalanceSuccess(val slots: List<SatsCardSlot>) : SatsCardSlotEvent()
    class ShowError(val e: Throwable?) : SatsCardSlotEvent()
    object Loading : SatsCardSlotEvent()
}