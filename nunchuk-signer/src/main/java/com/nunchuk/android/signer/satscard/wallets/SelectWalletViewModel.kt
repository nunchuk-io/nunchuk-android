package com.nunchuk.android.signer.satscard.wallets

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetSatsCardSlotKeyUseCase
import com.nunchuk.android.core.domain.SweepSatsCardSlotUseCase
import com.nunchuk.android.core.domain.UnsealSatsCardSlotUseCase
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.NewAddressUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectWalletViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val unsealSatsCardSlotUseCase: UnsealSatsCardSlotUseCase,
    private val sweepSatsCardSlotUseCase: SweepSatsCardSlotUseCase,
    private val getSatsCardSlotKeyUseCase: GetSatsCardSlotKeyUseCase,
    private val getAddressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SelectWalletState())
    private val _event = Channel<SelectWalletEvent>(Channel.CONFLATED)

    val state = _state.asStateFlow()
    val event = _event.consumeAsFlow()

    init {
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .onStart { _event.send(SelectWalletEvent.Loading(true)) }
                .onException {
                    _event.send(SelectWalletEvent.ShowError(it.message))
                }
                .onCompletion { _event.send(SelectWalletEvent.Loading(false)) }
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
            _event.send(SelectWalletEvent.NfcLoading(true))
            val result = unsealSatsCardSlotUseCase(UnsealSatsCardSlotUseCase.Data(isoDep, cvc))
            _event.send(SelectWalletEvent.NfcLoading(false))
            if (result.isSuccess) {
                getWalletAddress(listOf(result.getOrThrow()))
            } else {
                _event.send(SelectWalletEvent.ShowError(result.exceptionOrNull()?.message))
            }
        }
    }

    private fun getSlotsKey(isoDep: IsoDep, cvc: String, slots: List<SatsCardSlot>) {
        viewModelScope.launch {
            _event.send(SelectWalletEvent.NfcLoading(true))
            val result = getSatsCardSlotKeyUseCase(GetSatsCardSlotKeyUseCase.Data(isoDep, cvc, slots))
            _event.send(SelectWalletEvent.NfcLoading(false))
            if (result.isSuccess) {
                getWalletAddress(result.getOrThrow())
            }  else {
                _event.send(SelectWalletEvent.ShowError(result.exceptionOrNull()?.message))
            }
        }
    }

    private fun getWalletAddress(slots: List<SatsCardSlot>) {
        viewModelScope.launch {
            _event.send(SelectWalletEvent.Loading(true))
            getAddressesUseCase.execute(walletId = selectedWalletId)
                .flatMapLatest {
                    if (it.isEmpty()) {
                        return@flatMapLatest newAddressUseCase.execute(walletId = selectedWalletId).map { newAddress -> listOf(newAddress) }
                    }
                    return@flatMapLatest flowOf(it)
                }.onException {
                    _event.send(SelectWalletEvent.Loading(false))
                    _event.send(SelectWalletEvent.ShowError(it.message))
                }.collect {
                    sweepUnsealSlots(it.first(), slots)
                }

        }
    }

    private fun sweepUnsealSlots(address: String, slots: List<SatsCardSlot>) {
        viewModelScope.launch {
            val result = sweepSatsCardSlotUseCase(SweepSatsCardSlotUseCase.Data(address, slots))
            _event.send(SelectWalletEvent.Loading(false))
            if (result.isSuccess) {
                _event.send(SelectWalletEvent.SweepSuccess)
            }  else {
                _event.send(SelectWalletEvent.ShowError(result.exceptionOrNull()?.message))
            }
        }
    }

    val selectedWalletId: String
        get() = _state.value.selectedWalletId
}

sealed class SelectWalletEvent {
    object SweepSuccess : SelectWalletEvent()
    data class Loading(val isLoading: Boolean) : SelectWalletEvent()
    data class NfcLoading(val isLoading: Boolean) : SelectWalletEvent()
    data class ShowError(val message: String?) : SelectWalletEvent()
}

data class SelectWalletState(val selectWallets: List<SelectableWallet> = emptyList(), val selectedWalletId: String = "")