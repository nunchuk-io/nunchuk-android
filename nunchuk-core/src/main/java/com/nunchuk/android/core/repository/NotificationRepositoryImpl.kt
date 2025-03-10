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

package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.api.NotificationApi
import com.nunchuk.android.core.data.model.NotificationDeviceRequest
import com.nunchuk.android.repository.NotificationRepository
import javax.inject.Inject

internal class NotificationRepositoryImpl @Inject constructor(
    private val api: NotificationApi,
) : NotificationRepository {

    override suspend fun deviceRegister(token: String) {
        val response = api.deviceRegister(NotificationDeviceRequest(token))
        if (!response.isSuccess) {
            throw IllegalStateException("Failed to register device")
        }
    }

    override suspend fun deviceUnregister(token: String) {
        val response = api.deviceUnregister(NotificationDeviceRequest(token))
        if (!response.isSuccess) {
            throw IllegalStateException("Failed to unregister device")
        }
    }
}