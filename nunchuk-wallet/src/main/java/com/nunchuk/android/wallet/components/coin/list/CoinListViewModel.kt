package com.nunchuk.android.wallet.components.coin.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.coin.CoinCard
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.coin.LockCoinUseCase
import com.nunchuk.android.usecase.coin.UnLockCoinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val lockCoinUseCase: LockCoinUseCase,
    private val unLockCoinUseCase: UnLockCoinUseCase,
) : ViewModel() {
    private val args = CoinListFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _state = MutableStateFlow(CoinListUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<CoinListEvent>()
    val event = _event.asSharedFlow()

    init {
        getAllCoins()
    }

    private fun getAllCoins() {
        viewModelScope.launch {
            _event.emit(CoinListEvent.Loading(true))
            getAllCoinUseCase(args.walletId).onSuccess {
                val coins = it.map { output ->
                    CoinCard(
                        id = output.vout,
                        amount = output.amount,
                        isLocked = output.isLocked,
                        isChange = output.isChange,
                        note = output.memo,
                        tags = emptyList(),
                        time = output.time * 1000L,
                        txId = output.txid,
                        isScheduleBroadCast = output.scheduleTime > 0L,
                        status = output.status
                    )
                }
                _event.emit(CoinListEvent.Loading(false))
                _state.update { state ->
                    state.copy(coins = coins)
                }
            }
        }
    }

    fun onLockCoin() {
        viewModelScope.launch {
            val coins = state.value.selectedCoins
            coins.asSequence().filter { it.isLocked.not() }.forEach {
                lockCoinUseCase(LockCoinUseCase.Params(args.walletId, it.txId, it.id))
            }
            getAllCoins()
            _event.emit(CoinListEvent.CoinLocked)
            _state.update { it.copy(selectedCoins = emptySet(), mode = CoinListMode.NONE) }
        }
    }

    fun onUnlockCoin() {
        viewModelScope.launch {
            val coins = state.value.selectedCoins

            coins.asSequence().filter { it.isLocked }.forEach {
                unLockCoinUseCase(UnLockCoinUseCase.Params(args.walletId, it.txId, it.id))
            }
            getAllCoins()
            _event.emit(CoinListEvent.CoinUnlocked)
            _state.update { it.copy(selectedCoins = emptySet(), mode = CoinListMode.NONE) }
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

sealed class CoinListEvent {
    data class Loading(val isLoading: Boolean) : CoinListEvent()
    object CoinLocked : CoinListEvent()
    object CoinUnlocked : CoinListEvent()
}