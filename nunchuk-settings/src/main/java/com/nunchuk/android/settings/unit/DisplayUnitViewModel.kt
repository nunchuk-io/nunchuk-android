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

package com.nunchuk.android.settings.unit

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetDisplayUnitSettingUseCase
import com.nunchuk.android.core.domain.UpdateDisplayUnitSettingUseCase
import com.nunchuk.android.core.domain.data.DisplayUnitSetting
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DisplayUnitViewModel @Inject constructor(
    private val updateDisplayUnitSettingUseCase: UpdateDisplayUnitSettingUseCase,
    private val getDisplayUnitSettingUseCase: GetDisplayUnitSettingUseCase
) : NunchukViewModel<DisplayUnitState, DisplayUnitEvent>() {

    override val initialState = DisplayUnitState()

    val currentDisplayUnitSettings: DisplayUnitSetting?
        get() = state.value?.displayUnitSetting

    fun getDisplayUnitSetting() {
        viewModelScope.launch {
            getDisplayUnitSettingUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(displayUnitSetting = it)
                    }

                }
        }
    }

    fun updateDisplayUnitSetting(displayUnitSetting: DisplayUnitSetting) {
        viewModelScope.launch {
            updateDisplayUnitSettingUseCase.execute(displayUnitSetting)
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(displayUnitSetting = it)
                    }
                    event(DisplayUnitEvent.UpdateDisplayUnitSettingSuccessEvent(it))
                }
        }
    }

}