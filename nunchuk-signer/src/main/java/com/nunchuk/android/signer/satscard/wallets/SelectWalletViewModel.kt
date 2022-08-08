package com.nunchuk.android.signer.satscard.wallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetSatsCardSlotKeyUseCase
import com.nunchuk.android.core.domain.SweepSatsCardSlotUseCase
import com.nunchuk.android.core.domain.UnsealSatsCardSlotUseCase
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.NewAddressUseCase
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
    private val getSatsCardSlotKeyUseCase: GetSatsCardSlotKeyUseCase,
    private val getAddressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val getFeeUseCase: EstimateFeeUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SelectWalletState())
    private val _event = MutableSharedFlow<SelectWalletEvent>()

    val state = _state.asStateFlow()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .onStart { _event.emit(SelectWalletEvent.Loading(true)) }
                .onException {
                    _event.emit(SelectWalletEvent.Error(it))
                }
                .onCompletion { _event.emit(SelectWalletEvent.Loading(false)) }
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

    fun getWalletAddress() {
        viewModelScope.launch {
            _event.emit(SelectWalletEvent.Loading(true))
            getAddressesUseCase.execute(walletId = selectedWalletId)
                .flatMapLatest {
                    if (it.isEmpty()) {
                        return@flatMapLatest newAddressUseCase.execute(walletId = selectedWalletId).map { newAddress -> listOf(newAddress) }
                    }
                    return@flatMapLatest flowOf(it)
                }.onException {
                    _event.emit(SelectWalletEvent.Loading(false))
                    _event.emit(SelectWalletEvent.Error(it))
                }
                .collect {
                    _event.emit(SelectWalletEvent.Loading(false))
                    _event.emit(SelectWalletEvent.GetAddressSuccess(it.first()))
                }
        }
    }

    private fun sweepUnsealSlots(address: String, slots: List<SatsCardSlot>) {
        viewModelScope.launch {
            val estimateFeeResult = getFeeUseCase(Unit)
            if (estimateFeeResult.isSuccess) {
                val result = sweepSatsCardSlotUseCase(SweepSatsCardSlotUseCase.Data(address, slots, estimateFeeResult.getOrThrow().standardRate))
                _event.emit(SelectWalletEvent.Loading(false))
                if (result.isSuccess) {
                    _event.emit(SelectWalletEvent.SweepSuccess)
                } else {
                    _event.emit(SelectWalletEvent.Error(result.exceptionOrNull()))
                }
            } else {
                _event.emit(SelectWalletEvent.Loading(false))
                _event.emit(SelectWalletEvent.Error(estimateFeeResult.exceptionOrNull()))
            }
        }
    }

    val selectedWalletId: String
        get() = _state.value.selectedWalletId
}

sealed class SelectWalletEvent {
    object SweepSuccess : SelectWalletEvent()
    data class GetAddressSuccess(val address: String) : SelectWalletEvent()
    data class Loading(val isLoading: Boolean) : SelectWalletEvent()
    data class NfcLoading(val isLoading: Boolean) : SelectWalletEvent()
    data class Error(val e: Throwable?) : SelectWalletEvent()
}

data class SelectWalletState(val selectWallets: List<SelectableWallet> = emptyList(), val selectedWalletId: String = "")