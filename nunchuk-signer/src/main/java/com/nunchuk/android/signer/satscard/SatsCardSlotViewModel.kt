package com.nunchuk.android.signer.satscard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetSatsCardSlotBalanceUseCase
import com.nunchuk.android.listener.BlockListener
import com.nunchuk.android.model.NcExceptionCode
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.type.SatsCardSlotStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SatsCardSlotViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSatsCardSlotBalanceUseCase: GetSatsCardSlotBalanceUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<SatsCardSlotEvent>()
    private val _state = MutableStateFlow(
        SatsCardSlotState(
            savedStateHandle.get<SatsCardStatus>(SatsCardArgs.EXTRA_SATSCARD_STATUS)!!,
            isLoading = false,
            isSuccess = false,
            isNetworkError = false
        )
    )
    val event = _event.asSharedFlow()
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            BlockListener.getBlockChainFlow().collect {
                loadActiveSlotBalance()
            }
        }
        loadActiveSlotBalance()
        val status: SatsCardStatus = state.value.status
        viewModelScope.launch {
            val otherSlots = status.slots.filter { it.index != status.activeSlotIndex && it.status == SatsCardSlotStatus.UNSEALED }
            val result = getSatsCardSlotBalanceUseCase(otherSlots)
            if (result.isSuccess) {
                val previousStatus = state.value.status
                val activeSlot = previousStatus.slots.find { it.index == previousStatus.activeSlotIndex } ?: return@launch
                _state.value = _state.value.copy(status = previousStatus.copy(slots = result.getOrThrow() + activeSlot), isLoading = false)
                _event.emit(SatsCardSlotEvent.GetOtherSlotBalanceSuccess(result.getOrThrow()))
            } else {
                handleError(result.exceptionOrNull())
            }
        }
    }

    private fun loadActiveSlotBalance() {
        val status: SatsCardStatus = state.value.status
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val activeSlot = status.slots.find { it.index == status.activeSlotIndex } ?: return@launch
            val result = getSatsCardSlotBalanceUseCase(listOf(activeSlot))
            if (result.isSuccess) {
                val previousStatus = state.value.status
                val newSlot = result.getOrThrow().first()
                val newSlots = previousStatus.slots.toMutableList().apply {
                    set(previousStatus.activeSlotIndex, newSlot)
                }
                _state.value = _state.value.copy(status = previousStatus.copy(slots = newSlots), isLoading = false, isSuccess = true)
            } else {
                handleError(result.exceptionOrNull())
            }
        }
    }

    private suspend fun handleError(e: Throwable?) {
        val isNetworkError = e?.message?.contains(NcExceptionCode.NETWORK_ERROR.toString()) == true
        _state.value = _state.value.copy(isLoading = false, isNetworkError = isNetworkError)
        if (isNetworkError.not()) {
            _event.emit(SatsCardSlotEvent.ShowError(e))
        }
    }

    fun getUnsealSlots() = state.value.status.slots.filter { it.status == SatsCardSlotStatus.UNSEALED }

    fun isHasUnsealSlotBalance() = state.value.status.slots.any { it.status == SatsCardSlotStatus.UNSEALED && it.balance.value > 0 }

    fun getActiveSlot() = state.value.status.slots.find { it.index == state.value.status.activeSlotIndex }
}

data class SatsCardSlotState(val status: SatsCardStatus, val isLoading: Boolean, val isSuccess: Boolean, val isNetworkError: Boolean)

sealed class SatsCardSlotEvent {
    class GetOtherSlotBalanceSuccess(val slots: List<SatsCardSlot>) : SatsCardSlotEvent()
    class ShowError(val e: Throwable?) : SatsCardSlotEvent()
}