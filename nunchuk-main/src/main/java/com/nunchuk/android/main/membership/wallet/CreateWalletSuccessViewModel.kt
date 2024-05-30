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

package com.nunchuk.android.main.membership.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.usecase.byzantine.GetGroupRemoteUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateWalletSuccessViewModel @Inject constructor(
    private val getGroupRemoteUseCase: GetGroupRemoteUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _event = MutableSharedFlow<CreateWalletSuccessEvent>()
    val event = _event.asSharedFlow()

    private val args = CreateWalletSuccessFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _state = MutableStateFlow(CreateWalletSuccessUiState(isReplaceWallet = args.replacedWalletId.isNotEmpty()))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getWalletDetail2UseCase(args.walletId)
                .onSuccess {
                    val is2Of4MultisigWallet = it.signers.size == GroupWalletType.TWO_OF_FOUR_MULTISIG.m
                            && it.totalRequireSigns == GroupWalletType.TWO_OF_FOUR_MULTISIG.n
                    _state.update { state ->
                        state.copy(is2Of4MultisigWallet = is2Of4MultisigWallet, walletName = it.name)
                    }
                }
        }
    }

    fun loadGroup(id: String) {
        viewModelScope.launch {
            getGroupRemoteUseCase(GetGroupRemoteUseCase.Params(id)).onSuccess { result ->
                _state.update {
                    it.copy(
                        isSingleSetup = result.isSinglePersonSetup(),
                        allowInheritance = result.walletConfig.allowInheritance
                    )
                }
            }
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(CreateWalletSuccessEvent.ContinueStepEvent(state.value.is2Of4MultisigWallet))
        }
    }
}

data class CreateWalletSuccessUiState(
    val isSingleSetup: Boolean = false,
    val allowInheritance: Boolean = false,
    val is2Of4MultisigWallet: Boolean = false,
    val walletName: String = "",
    val isReplaceWallet: Boolean = false
)

sealed class CreateWalletSuccessEvent {
    data class ContinueStepEvent(val is2Of4MultisigWallet: Boolean) : CreateWalletSuccessEvent()
}