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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningParam
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MagicalPhraseIntroViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager,
    private val getInheritanceUseCase: GetInheritanceUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<MagicalPhraseIntroEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(MagicalPhraseIntroState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    fun init(param: InheritancePlanningParam.SetupOrReview) {
        getInheritance(param)
    }

    private fun getInheritance(param: InheritancePlanningParam.SetupOrReview) =
        viewModelScope.launch {
            _event.emit(MagicalPhraseIntroEvent.Loading(true))
            val result = getInheritanceUseCase(
                GetInheritanceUseCase.Param(
                    walletId = param.walletId,
                    groupId = param.groupId
                )
            )
            _event.emit(MagicalPhraseIntroEvent.Loading(false))
            if (result.isSuccess) {
                val inheritance = result.getOrThrow()
                _state.update {
                    it.copy(
                        magicalPhrase = inheritance.magic,
                        inheritanceKeys = inheritance.inheritanceKeys.map { key -> key.xfp }
                    )
                }
            } else {
                _event.emit(MagicalPhraseIntroEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }

    fun onContinueClicked() = viewModelScope.launch {
        if (_state.value.magicalPhrase.isNullOrBlank()) return@launch
        _event.emit(
            MagicalPhraseIntroEvent.OnContinueClicked(
                magicalPhrase = _state.value.magicalPhrase.orEmpty(),
                inheritanceKeys = _state.value.inheritanceKeys
            )
        )
    }
}

sealed class MagicalPhraseIntroEvent {
    data class Loading(val loading: Boolean) : MagicalPhraseIntroEvent()
    data class Error(val message: String) : MagicalPhraseIntroEvent()
    data class OnContinueClicked(
        val magicalPhrase: String,
        val inheritanceKeys: List<String>
    ) : MagicalPhraseIntroEvent()
}

data class MagicalPhraseIntroState(
    val magicalPhrase: String? = null,
    val inheritanceKeys: List<String> = emptyList(),
)