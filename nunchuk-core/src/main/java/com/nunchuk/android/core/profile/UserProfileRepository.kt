package com.nunchuk.android.core.profile

import com.nunchuk.android.core.network.ApiInterceptedException
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface UserProfileRepository {

    fun getUserProfile(): Flow<UserProfileResponse>

    fun confirmDeleteAccount(confirmationCode: String): Flow<Unit>

    fun requestDeleteAccount(): Flow<Unit>

    fun updateUserProfile(name: String?, avatarUrl: String?): Flow<UserProfileResponse>

    fun signOut(): Flow<Unit>

    fun getUserDevices(): Flow<List<UserDeviceResponse>>

    fun deleteDevices(devices: List<String>): Flow<Unit>

    fun compromiseDevices(devices: List<String>): Flow<Unit>
}

internal class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileApi: UserProfileApi,
    private val ncDataStore: NcDataStore,
) : UserProfileRepository {

    override fun getUserProfile() = flow {
        emit(userProfileApi.getUserProfile().data.user)
    }.flowOn(Dispatchers.IO)

    override fun confirmDeleteAccount(confirmationCode: String) = flow {
        val error = userProfileApi.confirmDeleteAccount(DeleteConfirmationPayload(confirmationCode)).getError()
        if (error != null) throw error
        emit(Unit)
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

    override fun updateUserProfile(name: String?, avatarUrl: String?) = flow {
        val payload = UpdateUserProfilePayload(name = name, avatarUrl = avatarUrl)
        emit(userProfileApi.updateUserProfile(payload).data.user)
    }.flowOn(Dispatchers.IO)

    override fun signOut() = flow {
        ncDataStore.clear()
        userProfileApi.signOut()
        emit(Unit)
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

}