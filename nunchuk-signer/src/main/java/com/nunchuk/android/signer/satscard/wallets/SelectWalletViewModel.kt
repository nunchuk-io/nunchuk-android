package com.nunchuk.android.signer.satscard.wallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.model.defaultRate
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
    private val getAddressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase,
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
        _state.value = _state.value.copy(selectWallets = selectWallets, selectedWalletId = walletId)
    }

    fun getWalletAddress(isCreateTransaction: Boolean) {
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
                    _event.emit(SelectWalletEvent.GetAddressSuccess(it.first(), isCreateTransaction))
                    _state.value = _state.value.copy(selectWalletAddress = it.first())
                }
        }
    }

    fun getEstimateFeeRates() {
        viewModelScope.launch {
            _event.emit(SelectWalletEvent.Loading(true))
            val result = estimateFeeUseCase(Unit)
            _event.emit(SelectWalletEvent.Loading(false))
            if (result.isSuccess) {
                _state.value = _state.value.copy(feeRates = result.getOrThrow())
                _event.emit((SelectWalletEvent.GetFeeRateSuccess(result.getOrThrow())))
            } else {
                _event.emit(SelectWalletEvent.Error(result.exceptionOrNull()))
            }
        }
    }

    val selectedWalletId: String
        get() = _state.value.selectedWalletId

    val selectWalletAddress: String
        get() = _state.value.selectWalletAddress

    val manualFeeRate: Int
        get() = _state.value.feeRates.defaultRate
}

sealed class SelectWalletEvent {
    data class GetAddressSuccess(val address: String, val isCreateTransaction: Boolean) : SelectWalletEvent()
    data class GetFeeRateSuccess(val estimateFeeRates: EstimateFeeRates) : SelectWalletEvent()
    data class Loading(val isLoading: Boolean) : SelectWalletEvent()
    data class Error(val e: Throwable?) : SelectWalletEvent()
}

data class SelectWalletState(
    val selectWallets: List<SelectableWallet> = emptyList(),
    val selectedWalletId: String = "",
    val feeRates: EstimateFeeRates = EstimateFeeRates(),
    val selectWalletAddress: String = ""
)