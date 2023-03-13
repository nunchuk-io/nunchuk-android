package com.nunchuk.android.wallet.components.coin.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.coin.CoinCard
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllCoinUseCase: GetAllCoinUseCase
) : ViewModel() {
    private val args = CoinListFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _state = MutableStateFlow(CoinListUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<CoinListEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            getAllCoinUseCase(args.walletId).onSuccess {
                val coins = it.map { output ->
                    CoinCard(
                        id = output.vout,
                        amount = output.amount,
                        isLocked = output.isLocked,
                        isChange = output.isChange,
                        note = output.memo,
                        tags = emptyList(),
                        time = System.currentTimeMillis(),
                        txId = output.txid,
                        isScheduleBroadCast = true
                    )
                }
                _state.update { state ->
                    state.copy(coins = coins)
                }
            }
        }
    }

    fun enableSelectMode() {
        _state.update { it.copy(mode = CoinListMode.SELECT) }
    }

    fun enableSearchMode() {
        _state.update { it.copy(mode = CoinListMode.SEARCH) }
    }

    fun onSelectOrUnselectAll(isSelect: Boolean) {
        _state.update {
            it.copy(
                selectedCoins = if (isSelect) state.value.coins.toSet() else emptySet()
            )
        }
    }

    fun onSelectDone() {
        _state.update { it.copy(mode = CoinListMode.NONE) }
    }

    fun onCoinSelect(coin: CoinCard, isSelect: Boolean) {
        val selectedCoins = state.value.selectedCoins.toMutableSet()
        if (isSelect) selectedCoins.add(coin) else selectedCoins.remove(coin)
        _state.update {
            it.copy(
                selectedCoins = selectedCoins
            )
        }
    }
}

sealed class CoinListEvent