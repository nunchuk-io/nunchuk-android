package com.nunchuk.android.core.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.nunchuk.android.core.network.ApiInterceptedException
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.profile.CompromiseDevicesPayload
import com.nunchuk.android.core.profile.DeleteConfirmationPayload
import com.nunchuk.android.core.profile.DeleteDevicesPayload
import com.nunchuk.android.core.profile.UpdateUserProfilePayload
import com.nunchuk.android.core.profile.UserProfileApi
import com.nunchuk.android.core.profile.UserProfileResponse
import com.nunchuk.android.core.profile.UserRepository
import com.nunchuk.android.utils.CrashlyticsReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class UserRepositoryImpl @Inject constructor(
    private val userProfileApi: UserProfileApi,
    private val ncDataStore: NcDataStore,
    @ApplicationContext private val context: Context,
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
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
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

    override suspend fun clearDataStore() = ncDataStore.clear()
}