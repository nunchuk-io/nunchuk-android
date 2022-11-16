/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.signer.software.components.create.CreateNewSeedEvent.GenerateMnemonicCodeErrorEvent
import com.nunchuk.android.signer.software.components.create.CreateNewSeedEvent.OpenSelectPhraseEvent
import com.nunchuk.android.usecase.GenerateMnemonicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CreateNewSeedViewModel @Inject constructor(
    private val generateMnemonicUseCase: GenerateMnemonicUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<CreateNewSeedEvent>()
    private val _state = MutableStateFlow(CreateNewSeedState())
    val event = _event.asSharedFlow()
    val state = _state.asStateFlow()

    fun init() {
        viewModelScope.launch {
            when (val result = generateMnemonicUseCase.execute()) {
                is Success -> _state.value = _state.value.copy(seeds = result.data.toPhrases(), mnemonic = result.data)
                is Error -> _event.emit(GenerateMnemonicCodeErrorEvent(result.exception.message.orUnknownError()))
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

internal fun String.toPhrases() = this.split(PHRASE_SEPARATOR).mapIndexed { index, s -> "${index.toCountable()}. $s" }

internal const val PHRASE_SEPARATOR = " "
