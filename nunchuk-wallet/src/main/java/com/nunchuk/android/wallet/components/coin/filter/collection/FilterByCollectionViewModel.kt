package com.nunchuk.android.wallet.components.coin.filter.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinCollectionAddition
import com.nunchuk.android.model.UnspentOutput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class FilterByCollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(FilterByTagUiState())
    val state = _state.asStateFlow()

    private val args: FilterByCollectionFragmentArgs =
        FilterByCollectionFragmentArgs.fromSavedStateHandle(savedStateHandle)

    fun extractTagAndNumberOfCoin(coins: List<UnspentOutput>, collections: List<CoinCollection>) {
        val numberOfCoinByCollectionId = mutableMapOf<Int, Int>()
        coins.forEach { output ->
            output.collection.forEach { collectionId ->
                numberOfCoinByCollectionId[collectionId] = numberOfCoinByCollectionId.getOrPut(collectionId) { 0 } + 1
            }
        }
        val tagAdditions = collections.map { CoinCollectionAddition(it, numberOfCoinByCollectionId[it.id] ?: 0) }
            .sortedBy { it.collection.name }
        val selectedIds = if (args.collectionIds.isEmpty()) collections.map { it.id }.toSet()
        else args.collectionIds.toSet()
        _state.update { it.copy(allCollections = tagAdditions, selectedTags = selectedIds) }
    }

    fun onCheckedChange(id: Int, isChecked: Boolean) {
        val newSet = _state.value.selectedTags.toMutableSet()
        if (isChecked) {
            newSet.add(id)
        } else {
            newSet.remove(id)
        }
        _state.update { it.copy(selectedTags = newSet) }
    }

    fun toggleSelected(isSelectAll: Boolean) {
        if (isSelectAll) {
            _state.update { it.copy(selectedTags = emptySet()) }
        } else {
            _state.update {
                it.copy(selectedTags = state.value.allCollections.map { tag -> tag.collection.id }.toSet())
            }
        }
    }

    fun getSelectCollections(): List<Int> {
        return if (state.value.allCollections.size == state.value.selectedTags.size) emptyList()
        else state.value.selectedTags.toList()
    }
}

data class FilterByTagUiState(
    val allCollections: List<CoinCollectionAddition> = emptyList(),
    val selectedTags: Set<Int> = emptySet()
)