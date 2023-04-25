package com.nunchuk.android.wallet.components.coin.search

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.fromCurrencyToBTC
import com.nunchuk.android.core.util.getBtcSat
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.components.coin.filter.CoinFilterUiState
import com.nunchuk.android.wallet.components.coin.list.CoinListMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CoinSearchViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args: CoinSearchFragmentArgs =
        CoinSearchFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CoinSearchFragmentEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinSearchUiState())
    val state = _state.asStateFlow()

    private val allCoins = mutableListOf<UnspentOutput>()
    private val allTags = hashMapOf<Int, CoinTag>()
    private val allCollections = hashMapOf<Int, CoinCollection>()

    val queryState = mutableStateOf("")

    private val mutex = Mutex()

    private val defaultFilter = CoinFilterUiState(showLockedCoin = isCustomizeCoinFlow.not())
    val filter = savedStateHandle.getStateFlow(KEY_FILTER, defaultFilter)

    fun updateFilter(filter: CoinFilterUiState) {
        savedStateHandle[KEY_FILTER] = filter
        viewModelScope.launch {
            handleSearch(queryState.value)
        }
    }

    fun setSelectedCoin(coins: Array<UnspentOutput>) {
        _state.update {
            it.copy(selectedCoins = coins.toSet())
        }
    }

    suspend fun update(
        coins: List<UnspentOutput>,
        tags: Map<Int, CoinTag>,
        collections: Map<Int, CoinCollection>
    ) {
        mutex.withLock {
            allCoins.apply {
                clear()
                addAll(coins)
            }
            allTags.apply {
                clear()
                putAll(tags)
            }
            allCollections.apply {
                clear()
                putAll(collections)
            }
        }
        if (queryState.value.isNotEmpty()) {
            handleSearch(queryState.value)
        }
    }

    suspend fun handleSearch(query: String) = withContext(ioDispatcher) {
        mutex.withLock {
            if (query.isEmpty() && !isCustomizeCoinFlow && !isFilteringOrSearch) {
                _state.update { it.copy(coins = emptyList()) }
            } else {
                val filter = filter.value
                val endTimeInSeconds =
                    if (filter.endTime > 0L) filter.endTime / 1000L else Long.MAX_VALUE
                val startTimeInSeconds = filter.startTime / 1000L
                val lowCaseQuery = query.lowercase().trim()

                val comparator = if (filter.isDescending)
                    compareByDescending<UnspentOutput> { it.amount.value }.thenBy { it.time }
                else compareBy<UnspentOutput> { it.amount.value }.thenBy { it.time }

                val minValue = filter.min.toDoubleOrNull() ?: 0.0
                val minSat =
                    if (filter.isMinBtc) minValue.getBtcSat() else minValue.fromCurrencyToBTC()
                        .toAmount().value

                val maxValue = filter.max.toDoubleOrNull() ?: Long.MAX_VALUE.toDouble()
                val maxSat =
                    if (filter.isMaxBtc) maxValue.getBtcSat() else maxValue.fromCurrencyToBTC()
                        .toAmount().value

                val queryTagIds =
                    allTags.asSequence()
                        .filter { it.value.name.lowercase().contains(lowCaseQuery) }
                        .map { it.key }.toSet()

                val queryCollectionIds =
                    allCollections.asSequence()
                        .filter { it.value.name.lowercase().contains(lowCaseQuery) }
                        .map { it.key }.toSet()

                val coins = allCoins
                    .asSequence()
                    .filter {
                        lowCaseQuery.isEmpty()
                                || it.tags.any { id -> queryTagIds.contains(id) }
                                || it.collection.any { id -> queryCollectionIds.contains(id) }
                                || it.memo.contains(lowCaseQuery)
                                || it.txid.contains(lowCaseQuery)
                                || it.address.contains(lowCaseQuery)
                                || it.amount.value.toString().contains(lowCaseQuery)
                    }
                    .filter {
                        filter.selectTags.isEmpty() || it.tags.any { tag ->
                            filter.selectTags.contains(
                                tag
                            )
                        }
                    }
                    .filter {
                        filter.selectCollections.isEmpty()
                                || it.collection.any { tag ->
                            filter.selectCollections.contains(tag)
                        }
                    }
                    .filter { it.amount.value in minSat..maxSat }
                    .filter { it.isLocked == filter.showLockedCoin || it.isLocked.not() == filter.showUnlockedCoin }
                    .filter { it.time in startTimeInSeconds..endTimeInSeconds }
                    .sortedWith(comparator)
                    .toList()
                _state.update { it.copy(coins = coins) }
            }
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

    fun resetSelect() {
        _state.update { it.copy(selectedCoins = emptySet(), mode = CoinListMode.NONE) }
    }

    fun getSelectedCoins() = _state.value.selectedCoins.toList()

    private val isCustomizeCoinFlow: Boolean
        get() = !args.inputs.isNullOrEmpty()

    val isFilteringOrSearch: Boolean
        get() = defaultFilter != filter.value || queryState.value.isNotEmpty()

    val isFiltering: Boolean
        get() = defaultFilter != filter.value

    companion object {
        private const val KEY_FILTER = "filter"
    }
}

sealed class CoinSearchFragmentEvent