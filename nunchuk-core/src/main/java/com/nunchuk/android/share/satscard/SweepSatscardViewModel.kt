package com.nunchuk.android.share.satscard

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetSatsCardSlotKeyUseCase
import com.nunchuk.android.core.domain.SweepSatsCardSlotUseCase
import com.nunchuk.android.core.domain.UnsealSatsCardSlotUseCase
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SweepSatscardViewModel @Inject constructor(
    private val unsealSatsCardSlotUseCase: UnsealSatsCardSlotUseCase,
    private val sweepSatsCardSlotUseCase: SweepSatsCardSlotUseCase,
    private val getSatsCardSlotKeyUseCase: GetSatsCardSlotKeyUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<SweepEvent>()
    val event = _event.asSharedFlow()

    private lateinit var address: String
    private var manualFeeRate: Int = 0

    fun init(address: String, manualFeeRate: Int) {
        this.address = address
        this.manualFeeRate = manualFeeRate
    }

    fun handleSweepBalance(isoDep: IsoDep?, cvc: String, slots: List<SatsCardSlot>, type: SweepType) {
        isoDep ?: return
        when (type) {
            SweepType.UNSEAL_SWEEP_TO_NUNCHUK_WALLET,
            SweepType.UNSEAL_SWEEP_TO_EXTERNAL_ADDRESS -> unsealSweepActiveSlot(isoDep, cvc, slots)
            SweepType.SWEEP_TO_NUNCHUK_WALLET,
            SweepType.SWEEP_TO_EXTERNAL_ADDRESS -> getSlotsKey(isoDep, cvc, slots)
            SweepType.NONE -> Unit
        }
    }

    private fun unsealSweepActiveSlot(isoDep: IsoDep, cvc: String, slots: List<SatsCardSlot>) {
        if (slots.isEmpty()) return
        viewModelScope.launch {
            setEvent(SweepEvent.NfcLoading(true))
            val result = unsealSatsCardSlotUseCase(UnsealSatsCardSlotUseCase.Data(isoDep, cvc, slots.first()))
            setEvent(SweepEvent.NfcLoading(false))
            if (result.isSuccess) {
                sweepUnsealSlots(address, listOf(result.getOrThrow()))
            } else {
                setEvent(SweepEvent.Error(result.exceptionOrNull()))
            }
        }
    }

    private fun getSlotsKey(isoDep: IsoDep, cvc: String, slots: List<SatsCardSlot>) {
        viewModelScope.launch {
            setEvent(SweepEvent.NfcLoading(true))
            val result = getSatsCardSlotKeyUseCase(GetSatsCardSlotKeyUseCase.Data(isoDep, cvc, slots))
            setEvent(SweepEvent.NfcLoading(false))
            if (result.isSuccess) {
                sweepUnsealSlots(address, result.getOrThrow())
            } else {
                setEvent(SweepEvent.Error(result.exceptionOrNull()))
            }
        }
    }

    private fun sweepUnsealSlots(address: String, slots: List<SatsCardSlot>) {
        viewModelScope.launch {
            setEvent(SweepEvent.SweepLoadingEvent(true))
            val result = sweepSatsCardSlotUseCase(SweepSatsCardSlotUseCase.Data(address, slots, manualFeeRate))
            setEvent(SweepEvent.SweepLoadingEvent(false))
            if (result.isSuccess) {
                setEvent(SweepEvent.SweepSuccess(result.getOrThrow()))
            } else {
                setEvent(SweepEvent.Error(result.exceptionOrNull()))
            }
        }
    }

    private suspend fun setEvent(event: SweepEvent) {
        _event.emit(event)
    }
}

sealed class SweepEvent {
    data class SweepLoadingEvent(val isLoading: Boolean) : SweepEvent()
    data class SweepSuccess(val transaction: Transaction) : SweepEvent()
    data class Error(val e: Throwable?) : SweepEvent()
    data class NfcLoading(val isLoading: Boolean) : SweepEvent()
}