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

package com.nunchuk.android.core.device

import com.nunchuk.android.core.persistence.NCSharePreferences
import com.nunchuk.android.utils.DeviceManager
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DeviceManagerImpl @Inject constructor(
    private val ncSharePreferences: NCSharePreferences
) : DeviceManager {
    private var deviceId: String = ncSharePreferences.deviceId

    private fun generateDeviceId() = UUID.randomUUID().toString()

    private fun storeDeviceId(deviceId: String) {
        ncSharePreferences.deviceId = deviceId
    }

    override fun getDeviceId(): String {
        if (deviceId.isEmpty()) {
            deviceId = generateDeviceId()
            storeDeviceId(deviceId)
        }

        return deviceId
    }

}