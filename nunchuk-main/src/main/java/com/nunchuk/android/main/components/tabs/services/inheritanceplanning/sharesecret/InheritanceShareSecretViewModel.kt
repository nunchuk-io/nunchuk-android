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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceShareSecretViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager,
) : ViewModel() {

    private val _event = MutableSharedFlow<InheritanceShareSecretEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceShareSecretState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    fun onContinueClick() = viewModelScope.launch {
        val option = _state.value.options.first { it.isSelected }
        _event.emit(InheritanceShareSecretEvent.ContinueClick(option.type))
    }

    fun onOptionClick(type: Int) {
        val value = _state.value
        val options = value.options.toMutableList()
        val newOptions = options.map {
            it.copy(isSelected = it.type == type)
        }
        _state.update {
            it.copy(options = newOptions)
        }
    }

}