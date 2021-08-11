package com.nunchuk.android.auth.data

import com.nunchuk.android.auth.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface AuthRepository {

    suspend fun register(name: String, email: String): UserTokenResponse

    fun login(email: String, password: String): Flow<UserTokenResponse>

    suspend fun changePassword(oldPassword: String, newPassword: String)

    suspend fun recoverPassword(email: String, oldPassword: String, newPassword: String)

    suspend fun forgotPassword(email: String)

    fun me(): Flow<UserResponse>
}

internal class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi
) : AuthRepository {

    override suspend fun register(name: String, email: String): UserTokenResponse {
        val payload = RegisterPayload(name, email)
        return authApi.register(payload).data
    }

    override fun login(email: String, password: String) = flow {
        val payload = SignInPayload(email = email, password = password)
        emit(authApi.signIn(payload).data)
    }.flowOn(Dispatchers.IO)

    override suspend fun changePassword(oldPassword: String, newPassword: String) {
        val payload = ChangePasswordPayload(oldPassword = oldPassword, newPassword = newPassword)
        authApi.changePassword(payload).data
    }

    override suspend fun recoverPassword(email: String, oldPassword: String, newPassword: String) {
        val payload = RecoverPasswordPayload(email = email, forgotPasswordToken = oldPassword, newPassword = newPassword)
        authApi.recoverPassword(payload).data
    }

    override suspend fun forgotPassword(email: String) {
        authApi.forgotPassword(ForgotPasswordPayload(email))
    }

    override fun me() = flow {
        emit(authApi.me().data.user)
    }.flowOn(Dispatchers.IO)

}