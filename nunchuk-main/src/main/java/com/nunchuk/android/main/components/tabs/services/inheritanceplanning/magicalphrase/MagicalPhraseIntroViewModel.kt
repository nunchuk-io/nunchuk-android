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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MagicalPhraseIntroViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager,
    savedStateHandle: SavedStateHandle,
    private val getInheritanceUseCase: GetInheritanceUseCase,
) : ViewModel() {
    private val args = MagicalPhraseIntroFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<MagicalPhraseIntroEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(MagicalPhraseIntroState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    init {
        getInheritance()
    }

    private fun getInheritance() = viewModelScope.launch {
        _event.emit(MagicalPhraseIntroEvent.Loading(true))
        val result = getInheritanceUseCase(args.walletId)
        _event.emit(MagicalPhraseIntroEvent.Loading(false))
        if (result.isSuccess) {
            _state.update { it.copy(magicalPhrase = result.getOrThrow().magic) }
        } else {
            _event.emit(MagicalPhraseIntroEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun onContinueClicked() = viewModelScope.launch {
        if (_state.value.magicalPhrase.isNullOrBlank()) return@launch
        _event.emit(MagicalPhraseIntroEvent.OnContinueClicked(_state.value.magicalPhrase.orEmpty()))
    }
}

sealed class MagicalPhraseIntroEvent {
    data class Loading(val loading: Boolean) : MagicalPhraseIntroEvent()
    data class Error(val message: String) : MagicalPhraseIntroEvent()
    data class OnContinueClicked(val magicalPhrase: String) : MagicalPhraseIntroEvent()
}

data class MagicalPhraseIntroState(
    val magicalPhrase: String? = null
)