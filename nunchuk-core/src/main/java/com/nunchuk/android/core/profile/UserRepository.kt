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

import com.nunchuk.android.core.network.ApiInterceptedException
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

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
    suspend fun checkShowOnboardForFreshInstall()
    suspend fun clearDataStore()
}

internal class UserRepositoryImpl @Inject constructor(
    private val userProfileApi: UserProfileApi,
    private val ncDataStore: NcDataStore,
) : UserRepository {

    override suspend fun getUserProfile(): UserProfileResponse {
        return userProfileApi.getUserProfile().data.user
    }

    override fun confirmDeleteAccount(confirmationCode: String) = flow {
        val result =
            userProfileApi.confirmDeleteAccount(DeleteConfirmationPayload(confirmationCode))
        if (result.isSuccess) {
            emit(Unit)
        } else {
            throw result.error
        }
    }

    override fun requestDeleteAccount() = flow {
        emit(
            try {
                userProfileApi.requestDeleteAccount().data
            } catch (e: ApiInterceptedException) {
                CrashlyticsReporter.recordException(e)
            }
        )
    }

    override suspend fun updateUserProfile(name: String?, avatarUrl: String?): UserProfileResponse {
        val payload = UpdateUserProfilePayload(name = name, avatarUrl = avatarUrl)
        return userProfileApi.updateUserProfile(payload).data.user
    }

    override suspend fun sendSignOut() {
        userProfileApi.signOut()
    }

    override fun getUserDevices() = flow {
        emit(
            userProfileApi.getUserDevices().data.devices
        )
    }

    override fun deleteDevices(devices: List<String>) = flow {
        userProfileApi.deleteUserDevices(DeleteDevicesPayload(devices = devices))
        emit(Unit)
    }

    override fun compromiseDevices(devices: List<String>) = flow {
        userProfileApi.compromiseUserDevices(CompromiseDevicesPayload(devices = devices))
        emit(Unit)
    }

    override fun showOnBoard(): Flow<Boolean?> = ncDataStore.showOnBoard

    override suspend fun setShowOnBoard(isShow: Boolean) = ncDataStore.setShowOnBoard(isShow)
    override suspend fun checkShowOnboardForFreshInstall() =
        ncDataStore.checkShowOnboardForFreshInstall()

    override suspend fun clearDataStore() = ncDataStore.clear()
}