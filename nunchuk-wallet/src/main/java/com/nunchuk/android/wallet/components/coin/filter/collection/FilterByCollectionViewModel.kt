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
        val selectedIds = args.collectionIds.toSet()
        _state.update { it.copy(allCollections = tagAdditions, previousCollectionIds = selectedIds, selectedIds = _state.value.selectedIds.ifEmpty { selectedIds }) }
    }

    fun onCheckedChange(id: Int, isChecked: Boolean) {
        val newSet = _state.value.selectedIds.toMutableSet()
        if (isChecked) {
            newSet.add(id)
        } else {
            newSet.remove(id)
        }
        _state.update { it.copy(selectedIds = newSet) }
    }

    fun toggleSelected(isSelectAll: Boolean) {
        if (isSelectAll) {
            _state.update { it.copy(selectedIds = emptySet()) }
        } else {
            _state.update {
                it.copy(selectedIds = state.value.allCollections.map { tag -> tag.collection.id }.toSet())
            }
        }
    }

    fun getSelectCollections(): List<Int> = state.value.selectedIds.toList()
}

data class FilterByTagUiState(
    val allCollections: List<CoinCollectionAddition> = emptyList(),
    val previousCollectionIds: Set<Int> = emptySet(),
    val selectedIds: Set<Int> = emptySet()
)