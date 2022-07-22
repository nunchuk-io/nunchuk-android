package com.nunchuk.android.signer.satscard.wallets

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetSatsCardSlotKeyUseCase
import com.nunchuk.android.core.domain.SweepSatsCardSlotUseCase
import com.nunchuk.android.core.domain.UnsealSatsCardSlotUseCase
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectWalletViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val unsealSatsCardSlotUseCase: UnsealSatsCardSlotUseCase,
    private val sweepSatsCardSlotUseCase: SweepSatsCardSlotUseCase,
    private val getSatsCardSlotKeyUseCase: GetSatsCardSlotKeyUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(SelectWalletState())
    private val _event = MutableStateFlow<SelectWalletEvent?>(null)

    val state = _state.asStateFlow()
    val event = _event.asStateFlow().filterIsInstance<SelectWalletEvent>()

    init {
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .onStart { _event.value = SelectWalletEvent.Loading(true) }
                .onException {
                    _event.value = SelectWalletEvent.ShowError(it.message)
                }
                .onCompletion { _event.value = SelectWalletEvent.Loading(false) }
                .collect { wallets ->
                    _state.value = _state.value.copy(selectWallets = wallets.map { SelectableWallet(it.wallet, it.isShared) })
                }
        }
    }

    fun setWalletSelected(walletId: String) {
        val selectWallets =
            _state.value.selectWallets.map { selectableWallet -> selectableWallet.copy(isSelected = selectableWallet.wallet.id == walletId) }
        _state.value = SelectWalletState(selectWallets, walletId)
    }

    fun handleSweepBalance(isoDep: IsoDep?, cvc: String, slots: List<SatsCardSlot>, type: Int) {
        isoDep ?: return
        when (type) {
            SelectWalletFragment.TYPE_UNSEAL_SWEEP_ACTIVE_SLOT -> unsealSweepActiveSlot(isoDep, cvc)
            SelectWalletFragment.TYPE_SWEEP_UNSEAL_SLOT -> getSlotsKey(isoDep, cvc, slots)
        }
    }

    private fun unsealSweepActiveSlot(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            val result = unsealSatsCardSlotUseCase(UnsealSatsCardSlotUseCase.Data(isoDep, cvc))
            if (result.isSuccess) {
                sweepUnsealSlots(listOf(result.getOrThrow()))
            }
        }
    }

    private fun getSlotsKey(isoDep: IsoDep, cvc: String, slots: List<SatsCardSlot>) {
        viewModelScope.launch {
            val result = getSatsCardSlotKeyUseCase(GetSatsCardSlotKeyUseCase.Data(isoDep, cvc, slots))
            if (result.isSuccess) {
                sweepUnsealSlots(result.getOrThrow())
            }
        }
    }

    private fun sweepUnsealSlots(slots: List<SatsCardSlot>) {
        viewModelScope.launch {
//            val result = sweepSatsCardSlotUseCase(SweepSatsCardSlotUseCase.Data(slots))
        }
    }
}

sealed class SelectWalletEvent {
    data class Loading(val isLoading: Boolean) : SelectWalletEvent()
    data class ShowError(val message: String?) : SelectWalletEvent()
}

data class SelectWalletState(val selectWallets: List<SelectableWallet> = emptyList(), val selectedWalletId: String = "")