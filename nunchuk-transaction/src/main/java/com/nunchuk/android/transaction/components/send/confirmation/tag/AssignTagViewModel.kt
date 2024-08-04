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

package com.nunchuk.android.transaction.components.send.confirmation.tag

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.usecase.coin.AddToCoinTagUseCase
import com.nunchuk.android.usecase.coin.GetListCoinByTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssignTagViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addToCoinTagUseCase: AddToCoinTagUseCase,
    private val getCoinTagAdditionListUseCase: GetListCoinByTagUseCase,
    private val assistedWalletManager: AssistedWalletManager,
) : ViewModel() {
    private val tags =
        savedStateHandle.get<ArrayList<CoinTag>>(AssignTagFragment.KEY_TAGS).orEmpty()
    private val walletId = savedStateHandle.get<String>(AssignTagFragment.KEY_WALLET_ID).orEmpty()
    private val output = savedStateHandle.get<UnspentOutput>(AssignTagFragment.KEY_COIN)!!
    private val _event = MutableSharedFlow<AssignTagEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(AssignTagUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val tagAdditions = tags.map {
                CoinTagAddition(
                    coinTag = it,
                    getCoinTagAdditionListUseCase(
                        GetListCoinByTagUseCase.Param(
                            walletId,
                            it.id
                        )
                    ).getOrNull().orEmpty().size
                )
            }
            _state.update { it.copy(tagAdditions = tagAdditions, selectedCoinTags = tags.map { tag -> tag.id }.toSet()) }
        }
    }

    fun onCheckedChange(id: Int, isChecked: Boolean) {
        val newSet = _state.value.selectedCoinTags.toMutableSet()
        if (isChecked) {
            newSet.add(id)
        } else {
            newSet.remove(id)
        }
        _state.update { it.copy(selectedCoinTags = newSet) }
    }

    fun toggleSelected(isSelectAll: Boolean) {
        if (isSelectAll) {
            _state.update { it.copy(selectedCoinTags = emptySet()) }
        } else {
            _state.update { it.copy(selectedCoinTags = tags.map { tag -> tag.id }.toSet()) }
        }
    }

    fun onAssignTag() {
        viewModelScope.launch {
            addToCoinTagUseCase(
                AddToCoinTagUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    tagIds = state.value.selectedCoinTags.toList(),
                    coins = listOf(output),
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId)
                )
            ).onSuccess {
                _event.emit(AssignTagEvent.AssignTagSuccess)
            }.onFailure {
                _event.emit(AssignTagEvent.AssignTagFailed(it.message.orUnknownError()))
            }
        }
    }
}

sealed class AssignTagEvent {
    object AssignTagSuccess : AssignTagEvent()
    data class AssignTagFailed(val message: String,) : AssignTagEvent()
}

data class AssignTagUiState(
    val tagAdditions: List<CoinTagAddition> = emptyList(),
    val selectedCoinTags: Set<Int> = emptySet(),
)