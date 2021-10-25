package com.nunchuk.android.core.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface UserProfileRepository {

    fun getUserProfile(): Flow<UserProfileResponse>

    fun updateUserProfile(name: String?, avatarUrl: String?): Flow<UserProfileResponse>
}

internal class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileApi: UserProfileApi
) : UserProfileRepository {

    override fun getUserProfile() = flow {
        emit(userProfileApi.getUserProfile().data.user)
    }.flowOn(Dispatchers.IO)

    override fun updateUserProfile(name: String?, avatarUrl: String?) = flow {
        val payload = UpdateUserProfilePayload(name = name, avatarUrl = avatarUrl)
        emit(userProfileApi.updateUserProfile(payload).data.user)
    }.flowOn(Dispatchers.IO)

}