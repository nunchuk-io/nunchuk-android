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

package com.nunchuk.android.settings.developer

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetDeveloperSettingUseCase
import com.nunchuk.android.core.domain.UpdateDeveloperSettingUseCase
import com.nunchuk.android.core.domain.data.DeveloperSetting
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DeveloperSettingViewModel @Inject constructor(
    private val updateDeveloperSettingUseCase: UpdateDeveloperSettingUseCase,
    private val getDeveloperSettingUseCase: GetDeveloperSettingUseCase
) : NunchukViewModel<DeveloperSettingState, DeveloperSettingEvent>() {

    override val initialState = DeveloperSettingState()

    fun getDeveloperSettings() {
        viewModelScope.launch {
            getDeveloperSettingUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(developerSetting = it)
                    }
                }
        }
    }

    fun updateDeveloperSettings(developerSetting: DeveloperSetting) {
        viewModelScope.launch {
            updateDeveloperSettingUseCase.execute(developerSetting)
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(developerSetting = it)
                    }
                    event(DeveloperSettingEvent.UpdateSuccessEvent(it))
                }
        }
    }

}