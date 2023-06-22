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

package com.nunchuk.android.wallet.components.coin.collectiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.usecase.coin.DeleteCoinCollectionUseCase
import com.nunchuk.android.usecase.coin.RemoveCoinFromCollectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinCollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deleteCoinCollectionUseCase: DeleteCoinCollectionUseCase,
    private val removeCoinFromCollectionUseCase: RemoveCoinFromCollectionUseCase,
    private val assistedWalletManager: AssistedWalletManager,
) : ViewModel() {

    private val _event = MutableSharedFlow<CoinCollectionDetailEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinCollectionDetailState())
    val state = _state.asStateFlow()

    private val args = CoinCollectionDetailFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        _state.update { it.copy(coinCollection = args.coinCollection) }
    }

    fun getListCoinByTag(
        allCoins: List<UnspentOutput>,
        tags: Map<Int, CoinTag>
    ) {
        val coins = allCoins.filter { it.collection.contains(args.coinCollection.id) }
        _state.update {
            it.copy(coins = coins, tags = tags)
        }
    }

    fun updateCoinCollection(coinCollection: CoinCollection) {
        _state.update { it.copy(coinCollection = coinCollection) }
    }

    fun deleteCoinCollection() = viewModelScope.launch {
        val result = deleteCoinCollectionUseCase(
            DeleteCoinCollectionUseCase.Param(
                walletId = args.walletId,
                collectionId = args.coinCollection.id,
                isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
            )
        )
        if (result.isSuccess) {
            _event.emit(CoinCollectionDetailEvent.DeleteCollectionSuccess)
        } else {
            _event.emit(CoinCollectionDetailEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun getNumCoins() = _state.value.coins.size

    fun getCoinCollection() = _state.value.coinCollection

    fun removeCoin(coins: List<UnspentOutput>) = viewModelScope.launch {
        val result = removeCoinFromCollectionUseCase(
            RemoveCoinFromCollectionUseCase.Param(
                walletId = args.walletId,
                collectionIds = listOf(args.coinCollection.id),
                coins = coins,
                isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
            )
        )
        if (result.isSuccess) {
            val coinList = _state.value.coins.toMutableList()
            coins.forEach {
                coinList.removeIf { coin ->
                    it.txid == coin.txid
                }
            }
            _state.update { it.copy(coins = coinList) }
            _event.emit(CoinCollectionDetailEvent.RemoveCoinSuccess)
        } else {
            _event.emit(CoinCollectionDetailEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}