package com.nunchuk.android.wallet.components.coin.filter

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class CoinFilterViewModel @Inject constructor(

) : ViewModel() {
    private val _event = MutableSharedFlow<CoinFilterEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinFilterUiState())
    val state = _state.asStateFlow()

    fun onApplyFilter() {

    }

    fun setSelectedTags(tagIds: IntArray) {
        _state.update { it.copy(selectTags = tagIds.toSet()) }
    }

    fun setSelectedCollection(collectionIds: IntArray) {
        _state.update { it.copy(selectCollections = collectionIds.toSet()) }
    }
}

sealed class CoinFilterEvent

data class CoinFilterUiState(
    val filters: List<CoinFilter> = emptyList(),
    val selectTags: Set<Int> = emptySet(),
    val selectCollections: Set<Int> = emptySet(),
    val isBtc: Boolean = true,
)