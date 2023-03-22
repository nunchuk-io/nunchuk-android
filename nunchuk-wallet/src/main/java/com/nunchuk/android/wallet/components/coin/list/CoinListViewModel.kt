package com.nunchuk.android.wallet.components.coin.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.usecase.coin.*
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
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val removeCoinFromTagUseCase: RemoveCoinFromTagUseCase,
) : ViewModel() {
    private val walletId = savedStateHandle.get<String>("wallet_id").orEmpty()
    private val _state = MutableStateFlow(CoinListUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<CoinListEvent>()
    val event = _event.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        getAllCoins()
        getAllTags()
        _state.update { it.copy(selectedCoins = emptySet(), mode = CoinListMode.NONE) }
    }

    private fun getAllCoins() {
        viewModelScope.launch {
            _event.emit(CoinListEvent.Loading(true))
            getAllCoinUseCase(walletId).onSuccess { coins ->
                _event.emit(CoinListEvent.Loading(false))
                _state.update { state ->
                    state.copy(coins = coins)
                }
            }
        }
    }

    private fun getAllTags() {
        viewModelScope.launch {
            getAllTagsUseCase(walletId).onSuccess { tags ->
                _state.update { state ->
                    state.copy(tags = tags.associateBy { it.id })
                }
            }
        }
    }

    fun onLockCoin(walletId: String) {
        viewModelScope.launch {
            val coins = state.value.selectedCoins
            coins.asSequence().filter { it.isLocked.not() }.forEach {
                lockCoinUseCase(LockCoinUseCase.Params(walletId, it.txid, it.vout))
            }
            getAllCoins()
            _event.emit(CoinListEvent.CoinLocked)
            _state.update { it.copy(selectedCoins = emptySet(), mode = CoinListMode.NONE) }
        }
    }

    fun onUnlockCoin(walletId: String) {
        viewModelScope.launch {
            val coins = state.value.selectedCoins

            coins.asSequence().filter { it.isLocked }.forEach {
                unLockCoinUseCase(UnLockCoinUseCase.Params(walletId, it.txid, it.vout))
            }
            getAllCoins()
            _event.emit(CoinListEvent.CoinUnlocked)
            _state.update { it.copy(selectedCoins = emptySet(), mode = CoinListMode.NONE) }
        }
    }

    fun removeCoin(walletId: String, tagId: Int) = viewModelScope.launch {
        val result = removeCoinFromTagUseCase(
            RemoveCoinFromTagUseCase.Param(
                walletId = walletId,
                tagId = tagId,
                coins = _state.value.selectedCoins.toList()
            )
        )
        if (result.isSuccess) {
            _event.emit(CoinListEvent.RemoveCoinSuccess)
        } else {
            _event.emit(CoinListEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
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
        _state.update { it.copy(mode = CoinListMode.NONE, selectedCoins = emptySet()) }
    }

    fun onCoinSelect(coin: UnspentOutput, isSelect: Boolean) {
        val selectedCoins = state.value.selectedCoins.toMutableSet()
        if (isSelect) selectedCoins.add(coin) else selectedCoins.remove(coin)
        _state.update {
            it.copy(
                selectedCoins = selectedCoins
            )
        }
    }

    fun getSelectedCoins() = _state.value.selectedCoins
}

sealed class CoinListEvent {
    data class Loading(val isLoading: Boolean) : CoinListEvent()
    data class Error(val message: String) : CoinListEvent()
    object CoinLocked : CoinListEvent()
    object CoinUnlocked : CoinListEvent()
    object RemoveCoinSuccess : CoinListEvent()
}