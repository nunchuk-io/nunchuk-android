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

package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownsuccess

import androidx.lifecycle.ViewModel
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.profile.UserProfileRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class EmergencyLockdownSuccessViewModel @Inject constructor(
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val appScope: CoroutineScope,
    private val repository: UserProfileRepository
) : ViewModel() {

    private val _event = MutableSharedFlow<EmergencyLockdownSuccessEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        appScope.launch {
            _event.emit(EmergencyLockdownSuccessEvent.Loading(true))
            repository.signOut()
                .flowOn(Dispatchers.IO)
                .onException {
                    _event.emit(EmergencyLockdownSuccessEvent.Loading(false))
                }
                .first()
            clearInfoSessionUseCase.invoke(Unit)
            _event.emit(EmergencyLockdownSuccessEvent.SignOut)
        }
    }

}