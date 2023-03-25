package com.nunchuk.android.wallet.components.coin.search

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.components.coin.list.CoinListMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CoinSearchFragmentViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _event = MutableSharedFlow<CoinSearchFragmentEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinSearchUiState())
    val state = _state.asStateFlow()

    private val allCoins = mutableListOf<UnspentOutput>()
    private val allTags = hashMapOf<Int, CoinTag>()

    val queryState = mutableStateOf("")

    private val mutex = Mutex()

    suspend fun update(coins: List<UnspentOutput>, tags: Map<Int, CoinTag>) {
        mutex.withLock {
            allCoins.apply {
                clear()
                addAll(coins)
            }
            allTags.apply {
                clear()
                putAll(tags)
            }
        }
        if (queryState.value.isNotEmpty()) {
            handleSearch(queryState.value)
        }
    }

    suspend fun handleSearch(query: String) = withContext(ioDispatcher) {
        mutex.withLock {
            val lowCaseQuery = query.lowercase()
            val queryTagIds = allTags.asSequence().filter { it.value.name.lowercase().contains(lowCaseQuery) }.map { it.key }.toSet()
            val coins = allCoins.filter { it.tags.intersect(queryTagIds).isNotEmpty() }
            _state.update { it.copy(coins = coins) }
        }
    }

    fun enableSelectMode() {
        _state.update { it.copy(mode = CoinListMode.SELECT) }
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

    fun resetSelect() {
        _state.update { it.copy(selectedCoins = emptySet(), mode = CoinListMode.NONE) }
    }
}

sealed class CoinSearchFragmentEvent