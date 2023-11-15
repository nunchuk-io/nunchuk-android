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

package com.nunchuk.android.wallet.components.coin.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinCollectionAddition
import com.nunchuk.android.usecase.coin.AddToCoinCollectionUseCase
import com.nunchuk.android.usecase.coin.RemoveCoinFromCollectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinCollectionListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addToCoinCollectionUseCase: AddToCoinCollectionUseCase,
    private val removeCoinFromCollectionUseCase: RemoveCoinFromCollectionUseCase,
    private val assistedWalletManager: AssistedWalletManager,
) :
    ViewModel() {

    val args = CoinCollectionListFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<CoinCollectionListEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinCollectionListState())
    val state = _state.asStateFlow()

    init {
        setPreSelectedCollections()
    }

    private fun setPreSelectedCollections() {
        val preSelectedCollections = hashSetOf<Int>()
        args.coins.forEach { output ->
            output.collection.forEach {
                preSelectedCollections.add(it)
            }
        }
        if (args.collectionFlow != CollectionFlow.MOVE) {
            _state.update {
                it.copy(
                    preSelectedCoinCollections = preSelectedCollections,
                    selectedCoinCollections = preSelectedCollections
                )
            }
        } else {
            _state.update {
                it.copy(
                    preSelectedCoinCollections = preSelectedCollections,
                    selectedCoinCollections = hashSetOf()
                )
            }
        }
    }

    fun updateCoins(allCoins: List<CoinCollection>, numberOfCoinByCollectionId: Map<Int, Int>) {
        val preSelectedCoinCollections = _state.value.preSelectedCoinCollections
        val collections = allCoins
            .filter {
                (args.collectionFlow == CollectionFlow.MOVE && preSelectedCoinCollections.contains(
                    it.id
                )).not()
            }.map { collection ->
                CoinCollectionAddition(
                    collection,
                    numberOfCoinByCollectionId[collection.id] ?: 0
                )
            }
        _state.update {
            it.copy(
                collections = collections
            )
        }
    }

    fun enableButtonSave(): Boolean {
        val selectedTags = _state.value.selectedCoinCollections
        val preSelectedTags = _state.value.preSelectedCoinCollections
        val addedTags = selectedTags.subtract(preSelectedTags)
        val deletedTags = preSelectedTags.subtract(selectedTags)
        return addedTags.isNotEmpty() || deletedTags.isNotEmpty()
    }

    fun onCheckedChange(id: Int, checked: Boolean) {
        val selectedCoinTags = _state.value.selectedCoinCollections.toMutableSet()
        if (args.collectionFlow == CollectionFlow.MOVE) {
            selectedCoinTags.clear()
            selectedCoinTags.add(id)
        } else {
            if (checked) {
                selectedCoinTags.add(id)
            } else {
                selectedCoinTags.remove(id)
            }
        }
        _state.update { it.copy(selectedCoinCollections = selectedCoinTags) }
    }

    fun addCoinCollection() = viewModelScope.launch {
        val selectedCollections = _state.value.selectedCoinCollections
        val preSelectedCollections = _state.value.preSelectedCoinCollections
        val deletedCollections = preSelectedCollections.subtract(selectedCollections)

        val addResultDefer = async {
            addToCoinCollectionUseCase(
                AddToCoinCollectionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(args.walletId),
                    walletId = args.walletId,
                    collectionIds = selectedCollections.toList(),
                    coins = args.coins.toList(),
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
                )
            )
        }
        val deleteResultDefer = async {
            removeCoinFromCollectionUseCase(
                RemoveCoinFromCollectionUseCase.Param(
                    groupId = assistedWalletManager.getGroupId(args.walletId),
                    walletId = args.walletId,
                    collectionIds = deletedCollections.toList(),
                    coins = args.coins.toList(),
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
                )
            )
        }
        val addResult = addResultDefer.await()
        val deleteResult = deleteResultDefer.await()
        if (addResult.isSuccess && deleteResult.isSuccess) {
            _event.emit(CoinCollectionListEvent.AddCoinToCollectionSuccess(args.coins.size))
        } else {
            val message = if (addResult.isFailure) {
                addResult.exceptionOrNull()?.message.orUnknownError()
            } else {
                deleteResult.exceptionOrNull()?.message.orUnknownError()
            }
            _event.emit(CoinCollectionListEvent.Error(message))
        }
    }
}