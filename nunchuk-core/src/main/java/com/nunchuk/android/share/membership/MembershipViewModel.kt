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

package com.nunchuk.android.share.membership

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.usecase.membership.RestartWizardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val restartWizardUseCase: RestartWizardUseCase,
    private val membershipStepManager: MembershipStepManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // don't change the value group_id
    private val groupId = savedStateHandle.get<String>("group_id").orEmpty()
    private val _event = MutableSharedFlow<MembershipEvent>()
    val event = _event.asSharedFlow()

    fun resetWizard(plan: MembershipPlan) {
        viewModelScope.launch {
            val result = restartWizardUseCase(RestartWizardUseCase.Param(plan, groupId))
            if (result.isSuccess) {
                membershipStepManager.restart()
                _event.emit(MembershipEvent.RestartWizardSuccess)
            }
        }
    }
}

sealed class MembershipEvent {
    object RestartWizardSuccess : MembershipEvent()
}