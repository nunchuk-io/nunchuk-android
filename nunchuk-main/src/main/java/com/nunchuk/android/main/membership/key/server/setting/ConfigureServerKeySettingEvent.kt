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

package com.nunchuk.android.main.membership.key.server.setting

import com.nunchuk.android.model.KeyPolicy

sealed class ConfigureServerKeySettingEvent {
    object NoDelayInput : ConfigureServerKeySettingEvent()
    object DelaySigningInHourInvalid : ConfigureServerKeySettingEvent()
    data class ShowError(val message: String) : ConfigureServerKeySettingEvent()
    data class Loading(val isLoading: Boolean) : ConfigureServerKeySettingEvent()
    data class ConfigServerSuccess(val keyPolicy: KeyPolicy, val isDecrease: Boolean = false) : ConfigureServerKeySettingEvent()
}

data class ConfigureServerKeySettingState(
    val isUpdate: Boolean = false,
    val cosigningTextHours: String = "",
    val cosigningTextMinutes: String = "",
    val autoBroadcastSwitched: Boolean = true,
    val enableCoSigningSwitched: Boolean = false,
) {
    companion object {
        val Empty = ConfigureServerKeySettingState()
    }
}