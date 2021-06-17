package com.nunchuk.android.auth.data

import com.nunchuk.android.auth.api.*
import javax.inject.Inject

interface AuthRepository {

    suspend fun register(name: String, email: String): UserTokenResponse

    suspend fun login(email: String, password: String): UserTokenResponse

    suspend fun changePassword(oldPassword: String, newPassword: String)

    suspend fun recoverPassword(email: String, oldPassword: String, newPassword: String)

    suspend fun forgotPassword(email: String)

    suspend fun me(): UserResponse
}

internal class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi
) : AuthRepository {

    override suspend fun register(name: String, email: String): UserTokenResponse {
        val payload = RegisterPayload(name, email)
        return authApi.register(payload).data
    }

    override suspend fun login(email: String, password: String): UserTokenResponse {
        val payload = SignInPayload(email = email, password = password)
        return authApi.signIn(payload).data
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String) {
        val payload = ChangePasswordPayload(oldPassword = oldPassword, newPassword = newPassword)
        authApi.changePassword(payload)
    }

    override suspend fun recoverPassword(email: String, oldPassword: String, newPassword: String) {
        val payload = RecoverPasswordPayload(email = email, forgotPasswordToken = oldPassword, newPassword = newPassword)
        authApi.recoverPassword(payload)
    }

    override suspend fun forgotPassword(email: String) {
        authApi.forgotPassword(ForgotPasswordPayload(email))
    }

    override suspend fun me() = authApi.me().data.user

}