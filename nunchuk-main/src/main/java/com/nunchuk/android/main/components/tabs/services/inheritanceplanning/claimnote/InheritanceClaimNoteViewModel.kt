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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claimnote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.GetInheritanceClaimStateUseCase
import com.nunchuk.android.core.util.BTC_SATOSHI_EXCHANGE_RATE
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceClaimNoteViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = InheritanceClaimNoteFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<InheritanceClaimNoteEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceClaimNote())
    val state = _state.asStateFlow()

    init {
        _state.update { it.copy(inheritanceAdditional = args.inheritanceAdditional) }
    }

    fun onWithdrawClick() = viewModelScope.launch {
        _event.emit(
            InheritanceClaimNoteEvent.WithdrawClick(
                _state.value.inheritanceAdditional?.balance ?: 0.0
            )
        )
    }

    fun checkWallet() = viewModelScope.launch {
        getWalletsUseCase.execute().flowOn(Dispatchers.IO)
            .onException { _event.emit(InheritanceClaimNoteEvent.Error(it.message.orUnknownError())) }
            .flowOn(Dispatchers.Main)
            .collect {
                _event.emit(InheritanceClaimNoteEvent.CheckHasWallet(it.isNotEmpty()))
            }
    }

    fun getBalance() = _state.value.inheritanceAdditional?.balance ?: 0.0

}