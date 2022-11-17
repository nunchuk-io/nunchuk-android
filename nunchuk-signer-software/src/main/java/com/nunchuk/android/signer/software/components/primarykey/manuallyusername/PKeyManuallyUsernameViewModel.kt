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

package com.nunchuk.android.signer.software.components.primarykey.manuallyusername

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.CheckUsernamePrimaryKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PKeyManuallyUsernameViewModel @Inject constructor(private val checkUsernamePrimaryKeyUseCase: CheckUsernamePrimaryKeyUseCase) :
    NunchukViewModel<PKeyManuallyUsernameState, PKeyManuallyUsernameEvent>() {

    override val initialState: PKeyManuallyUsernameState = PKeyManuallyUsernameState()

    fun updateUsername(username: String) {
        updateState { copy(username = username) }
    }

    fun handleContinue() = viewModelScope.launch {
        val username = getState().username
        if (username.isBlank()) return@launch
        setEvent(PKeyManuallyUsernameEvent.LoadingEvent(true))
        val result = checkUsernamePrimaryKeyUseCase(CheckUsernamePrimaryKeyUseCase.Param(username = username))
        setEvent(PKeyManuallyUsernameEvent.LoadingEvent(false))
        if (result.isSuccess) {
            setEvent(PKeyManuallyUsernameEvent.CheckUsernameSuccess(username))
        } else {
            setEvent(PKeyManuallyUsernameEvent.ProcessFailure)
        }
    }

}