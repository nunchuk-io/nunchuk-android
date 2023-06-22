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

package com.nunchuk.android.wallet.components.coin.detail.ancestry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.usecase.coin.GetCoinAncestryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinAncestryViewModel @Inject constructor(
    private val getCoinAncestryUseCase: GetCoinAncestryUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args: CoinAncestryFragmentArgs =
        CoinAncestryFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CoinAncestryEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinAncestryUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getCoinAncestryUseCase(GetCoinAncestryUseCase.Param(args.walletId, args.output.txid, args.output.vout)).onSuccess {
                val allCoins = it.toMutableList().apply {
                    add(0, listOf(args.output))
                }
                _state.update { state -> state.copy(coins = allCoins) }
            }
        }
    }
}

sealed class CoinAncestryEvent

data class CoinAncestryUiState(val coins: List<List<UnspentOutput>> = emptyList())