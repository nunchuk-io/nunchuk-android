package com.nunchuk.android.wallet.components.coin.filter.tag

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.model.UnspentOutput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class FilterByTagViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(FilterByTagUiState())
    val state = _state.asStateFlow()

    private val args: FilterByTagFragmentArgs =
        FilterByTagFragmentArgs.fromSavedStateHandle(savedStateHandle)

    fun extractTagAndNumberOfCoin(coins: List<UnspentOutput>, tags: List<CoinTag>) {
        val numberOfCoinByTagId = mutableMapOf<Int, Int>()
        coins.forEach { output ->
            output.tags.forEach { tagId ->
                numberOfCoinByTagId[tagId] = numberOfCoinByTagId.getOrPut(tagId) { 0 } + 1
            }
        }
        val tagAdditions = tags.map { CoinTagAddition(it, numberOfCoinByTagId[it.id] ?: 0) }
            .sortedBy { it.coinTag.name }
        val selectedIds = if (args.tagIds.isEmpty()) tags.map { it.id }.toSet()
        else args.tagIds.toSet()
        _state.update { it.copy(allTags = tagAdditions, selectedTags = selectedIds) }
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
                it.copy(selectedTags = state.value.allTags.map { tag -> tag.coinTag.id }.toSet())
            }
        }
    }

    fun getSelectTags(): List<Int> {
        return if (state.value.allTags.size == state.value.selectedTags.size) emptyList<Int>()
        else state.value.selectedTags.toList()
    }
}

data class FilterByTagUiState(
    val allTags: List<CoinTagAddition> = emptyList(),
    val selectedTags: Set<Int> = emptySet()
)