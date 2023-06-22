/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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
        val selectedIds = args.tagIds.toSet()
        _state.update { it.copy(allTags = tagAdditions, previousTags = selectedIds, selectedTags = _state.value.selectedTags.ifEmpty { selectedIds }) }
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

    fun getSelectTags(): List<Int> = state.value.selectedTags.toList()
}

data class FilterByTagUiState(
    val allTags: List<CoinTagAddition> = emptyList(),
    val previousTags: Set<Int> = emptySet(),
    val selectedTags: Set<Int> = emptySet()
)