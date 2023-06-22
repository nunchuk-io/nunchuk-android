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

package com.nunchuk.android.auth.data

import com.nunchuk.android.auth.api.*
import com.nunchuk.android.core.network.ApiInterceptedException
import com.nunchuk.android.model.setting.QrSignInData
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.DeviceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface AuthRepository {

    suspend fun register(name: String, email: String): UserTokenResponse

    fun login(email: String, password: String): Flow<UserTokenResponse>

    fun verify(
        email: String,
        loginHalfToken: String,
        pin: String,
        deviceId: String
    ): Flow<UserTokenResponse>

    fun changePassword(oldPassword: String, newPassword: String): Flow<Unit>

    suspend fun recoverPassword(email: String, oldPassword: String, newPassword: String)

    suspend fun forgotPassword(email: String)

    suspend fun tryLogin(qr: String) : QrSignInData

    suspend fun confirmLogin(uuid: String?, token: String)
}

internal class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val deviceManager: DeviceManager,
) : AuthRepository {

    override suspend fun register(name: String, email: String): UserTokenResponse {
        val payload = RegisterPayload(name, email)
        return authApi.register(payload).data
    }

    override fun login(email: String, password: String) = flow {
        val payload = SignInPayload(email = email, password = password)
        emit(authApi.signIn(payload).data)
    }.flowOn(Dispatchers.IO)

    override fun verify(
        email: String,
        loginHalfToken: String,
        pin: String,
        deviceId: String
    ) = flow {
        val payload = VerifyNewDevicePayload(
            email = email, loginHalfToken = loginHalfToken, pin = pin, deviceId = deviceId
        )
        emit(authApi.verifyNewDevice(payload).data)
    }.flowOn(Dispatchers.IO)

    override fun changePassword(oldPassword: String, newPassword: String) = flow {
        val payload = ChangePasswordPayload(oldPassword = oldPassword, newPassword = newPassword)
        emit(
            try {
                authApi.changePassword(payload).data
            } catch (e: ApiInterceptedException) {
                CrashlyticsReporter.recordException(e)
            }
        )
    }

    override suspend fun recoverPassword(email: String, oldPassword: String, newPassword: String) {
        val payload = RecoverPasswordPayload(email = email, forgotPasswordToken = oldPassword, newPassword = newPassword)
        try {
            authApi.recoverPassword(payload).data
        } catch (e: ApiInterceptedException) {
            CrashlyticsReporter.recordException(e)
        }
    }

    override suspend fun forgotPassword(email: String) {
        try {
            authApi.forgotPassword(ForgotPasswordPayload(email)).data
        } catch (e: ApiInterceptedException) {
            CrashlyticsReporter.recordException(e)
        }
    }

    override suspend fun confirmLogin(uuid: String?, token: String) {
        val response = authApi.confirmLogin(ConfirmQrLoginRequest(uuid = uuid, token = token))
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun tryLogin(qr: String): QrSignInData {
        val response = authApi.tryLogin(TryLoginRequest(uuid = deviceManager.getDeviceId(), qrCode = qr))
        return QrSignInData(
            uuid = response.data.uuid,
            token = response.data.token.orEmpty()
        )
    }
}