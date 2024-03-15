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

package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.api.UserTokenResponse
import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface VerifyNewDeviceUseCase {
    fun execute(
        email: String,
        loginHalfToken: String,
        pin: String,
        deviceId: String,
        staySignedIn: Boolean
    ): Flow<Pair<String, String>>
}

internal class VerifyNewDeviceUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val checkShowOnboardUseCase: CheckShowOnboardUseCase,
) : VerifyNewDeviceUseCase {

    override fun execute(
        email: String,
        loginHalfToken: String,
        pin: String,
        deviceId: String,
        staySignedIn: Boolean
    ) = authRepository.verify(
        email = email, loginHalfToken = loginHalfToken, pin = pin, deviceId = deviceId
    ).map {
        storeAccount(email, it, staySignedIn)
    }

    private suspend fun storeAccount(email: String, response: UserTokenResponse, staySignedIn: Boolean): Pair<String, String> {
        val account = accountManager.getAccount()
        accountManager.storeAccount(
            account.copy(
                email = email,
                token = response.tokenId,
                activated = true,
                staySignedIn = staySignedIn,
                deviceId = response.deviceId
            )
        )

        runCatching {
            getUserProfileUseCase(Unit)
            checkShowOnboardUseCase(Unit)
        }


        return response.tokenId to response.deviceId
    }

}