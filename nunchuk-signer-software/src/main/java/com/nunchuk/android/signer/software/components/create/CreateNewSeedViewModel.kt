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

package com.nunchuk.android.signer.software.components.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.signer.software.components.create.CreateNewSeedEvent.GenerateMnemonicCodeErrorEvent
import com.nunchuk.android.signer.software.components.create.CreateNewSeedEvent.OpenSelectPhraseEvent
import com.nunchuk.android.usecase.GenerateMnemonicUseCase
import com.nunchuk.android.usecase.wallet.GetHotWalletMnemonicUseCase
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
internal class CreateNewSeedViewModel @Inject constructor(
    private val generateMnemonicUseCase: GenerateMnemonicUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getHotWalletMnemonicUseCase: GetHotWalletMnemonicUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<CreateNewSeedEvent>()
    private val _state = MutableStateFlow(CreateNewSeedState())
    private val args = CreateNewSeedFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val event = _event.asSharedFlow()
    val state = _state.asStateFlow()

    fun init() {
        if (args.walletId.isNotEmpty() && args.replacedXfp.isEmpty()) {
            viewModelScope.launch {
                getHotWalletMnemonicUseCase(args.walletId)
                    .onSuccess { mnemonic ->
                        _state.update { state ->
                            state.copy(
                                seeds = mnemonic.toPhrases(),
                                mnemonic = mnemonic
                            )
                        }
                    }
            }
            viewModelScope.launch {
                getWalletDetail2UseCase(args.walletId)
                    .onSuccess { walletDetail ->
                        _state.update { state ->
                            state.copy(
                                masterSignerId = walletDetail.signers.first().masterSignerId
                            )
                        }
                    }
            }
        } else {
            viewModelScope.launch {
                val count = if (args.groupId.isNullOrEmpty() && args.replacedXfp.isEmpty()) 24 else 12
                generateMnemonicUseCase(count).onSuccess {
                    _state.update { state ->
                        state.copy(
                            seeds = it.toPhrases(),
                            mnemonic = it
                        )
                    }
                }.onFailure {
                    _event.emit(GenerateMnemonicCodeErrorEvent(it.message.orUnknownError()))
                }
            }
        }
    }

    fun handleContinueEvent() {
        viewModelScope.launch {
            _event.emit(OpenSelectPhraseEvent(_state.value.mnemonic))
        }
    }
}

private fun Int.toCountable() = (this + 1).let {
    if (it < 10) "0$it" else "$it"
}

internal fun String.toPhrases() =
    this.split(PHRASE_SEPARATOR).mapIndexed { index, s -> "${index.toCountable()}. $s" }

internal const val PHRASE_SEPARATOR = " "
