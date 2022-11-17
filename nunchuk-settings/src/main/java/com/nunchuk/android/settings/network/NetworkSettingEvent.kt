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

package com.nunchuk.android.settings.network

import com.nunchuk.android.model.AppSettings

data class NetworkSettingState(
    val appSetting: AppSettings = AppSettings()
)

sealed class NetworkSettingEvent {
    data class UpdateSettingSuccessEvent(val appSetting: AppSettings) : NetworkSettingEvent()
    data class ResetTextHostServerEvent(val appSetting: AppSettings) : NetworkSettingEvent()
    object SignOutSuccessEvent : NetworkSettingEvent()
    data class LoadingEvent(val loading: Boolean) : NetworkSettingEvent()
}