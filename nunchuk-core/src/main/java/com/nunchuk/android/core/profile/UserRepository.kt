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

package com.nunchuk.android.core.profile

import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun getUserProfile(): UserProfileResponse

    fun confirmDeleteAccount(confirmationCode: String): Flow<Unit>

    fun requestDeleteAccount(): Flow<Unit>

    suspend fun updateUserProfile(name: String?, avatarUrl: String?): UserProfileResponse

    suspend fun sendSignOut()

    fun getUserDevices(): Flow<List<UserDeviceResponse>>

    fun deleteDevices(devices: List<String>): Flow<Unit>

    fun compromiseDevices(devices: List<String>): Flow<Unit>

    fun showOnBoard(): Flow<Boolean?>

    suspend fun setShowOnBoard(isShow: Boolean)
    suspend fun clearDataStore()
}