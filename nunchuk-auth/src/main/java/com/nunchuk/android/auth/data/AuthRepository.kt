package com.nunchuk.android.auth.data

import com.nunchuk.android.auth.api.AuthApi
import com.nunchuk.android.auth.api.UserTokenResponse
import javax.inject.Inject

interface AuthRepository {

    suspend fun register(name: String, email: String): UserTokenResponse

    suspend fun login(email: String, password: String): UserTokenResponse

    suspend fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String)

    suspend fun recoverPassword(emailAddress: String, oldPassword: String, newPassword: String, confirmPassword: String)
}

internal class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi
) : AuthRepository {

    override suspend fun register(name: String, email: String) = authApi.register(name, email).data

    override suspend fun login(email: String, password: String) = authApi.signIn(email = email, password = password).data

    override suspend fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        authApi.changePassword(
            oldPwd = oldPassword,
            newPassword = newPassword,
            newPasswordConfirmed = confirmPassword
        )
    }

    override suspend fun recoverPassword(emailAddress: String, oldPassword: String, newPassword: String, confirmPassword: String) {
        authApi.recoverPassword(
            email = emailAddress,
            forgotPwdToken = oldPassword,
            newPassword = newPassword,
            newPasswordConfirmed = confirmPassword
        )
    }

}