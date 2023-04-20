package com.nunchuk.android.wallet.components.coin.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.coin.GetAllCollectionsUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.coin.LockCoinUseCase
import com.nunchuk.android.usecase.coin.RemoveCoinFromCollectionUseCase
import com.nunchuk.android.usecase.coin.RemoveCoinFromTagUseCase
import com.nunchuk.android.usecase.coin.UnLockCoinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val removeCoinFromCollectionUseCase: RemoveCoinFromCollectionUseCase,
    private val getAllCollectionsUseCase: GetAllCollectionsUseCase,
    private val assistedWalletManager: AssistedWalletManager,
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
        getAllCollections()
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

    private fun getAllCollections() {
        viewModelScope.launch {
            getAllCollectionsUseCase(walletId).onSuccess { collections ->
                _state.update { state ->
                    state.copy(collections = collections.associateBy { it.id })
                }
            }
        }
    }

    fun onLockCoin(walletId: String, selectedCoins: List<UnspentOutput>) {
        viewModelScope.launch {
            selectedCoins.asSequence().filter { it.isLocked.not() }.forEach {
                lockCoinUseCase(
                    LockCoinUseCase.Params(
                        walletId,
                        it.txid,
                        it.vout,
                        assistedWalletManager.isActiveAssistedWallet(walletId)
                    )
                )
            }
            getAllCoins()
            _event.emit(CoinListEvent.CoinLocked)
        }
    }

    fun onUnlockCoin(walletId: String, selectedCoins: List<UnspentOutput>) {
        viewModelScope.launch {
            selectedCoins.asSequence().filter { it.isLocked }.forEach {
                unLockCoinUseCase(
                    UnLockCoinUseCase.Params(
                        walletId,
                        it.txid,
                        it.vout,
                        assistedWalletManager.isActiveAssistedWallet(walletId)
                    )
                )
            }
            getAllCoins()
            _event.emit(CoinListEvent.CoinUnlocked)
        }
    }

    fun removeCoinFromTag(walletId: String, tagId: Int) = viewModelScope.launch {
        val result = removeCoinFromTagUseCase(
            RemoveCoinFromTagUseCase.Param(
                walletId = walletId,
                tagIds = listOf(tagId),
                coins = _state.value.selectedCoins.toList(),
                isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
            )
        )
        if (result.isSuccess) {
            _event.emit(CoinListEvent.RemoveCoinFromTagSuccess)
        } else {
            _event.emit(CoinListEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun removeCoinFromCollection(walletId: String, collectionId: Int) = viewModelScope.launch {
        val result = removeCoinFromCollectionUseCase(
            RemoveCoinFromCollectionUseCase.Param(
                walletId = walletId,
                collectionIds = listOf(collectionId),
                coins = _state.value.selectedCoins.toList(),
                isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
            )
        )
        if (result.isSuccess) {
            _event.emit(CoinListEvent.RemoveCoinFromCollectionSuccess)
        } else {
            _event.emit(CoinListEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun enableSelectMode() {
        _state.update { it.copy(mode = CoinListMode.SELECT) }
    }

    fun onSelectOrUnselectAll(isSelect: Boolean, coins: List<UnspentOutput>) {
        _state.update {
            it.copy(
                selectedCoins = if (isSelect) coins.toSet() else emptySet()
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

    fun resetSelect() {
        _state.update { it.copy(selectedCoins = emptySet(), mode = CoinListMode.NONE) }
    }

    fun getSelectedCoins() = _state.value.selectedCoins.toList()

    fun getLockedCoins() = _state.value.coins.filter { it.isLocked }
}

sealed class CoinListEvent {
    data class Loading(val isLoading: Boolean) : CoinListEvent()
    data class Error(val message: String) : CoinListEvent()
    object CoinLocked : CoinListEvent()
    object CoinUnlocked : CoinListEvent()
    object RemoveCoinFromTagSuccess : CoinListEvent()
    object RemoveCoinFromCollectionSuccess : CoinListEvent()
}