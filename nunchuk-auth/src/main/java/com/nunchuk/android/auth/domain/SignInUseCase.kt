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
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.domain.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface SignInUseCase {
    fun execute(
        email: String,
        password: String,
        staySignedIn: Boolean = true,
        fetchUserInfo: Boolean = true,
    ): Flow<AccountInfo>
}

internal class SignInUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val storeAccountUseCase: StoreAccountUseCase
) : SignInUseCase {

    override fun execute(
        email: String,
        password: String,
        staySignedIn: Boolean,
        fetchUserInfo: Boolean
    ): Flow<AccountInfo> =
        authRepository.login(
            email = email,
            password = password
        ).map {
            storeAccount(email, it, staySignedIn, fetchUserInfo)
        }.flowOn(ioDispatcher)

    private suspend fun storeAccount(
        email: String,
        response: UserTokenResponse,
        staySignedIn: Boolean,
        fetchUserInfo: Boolean
    ): AccountInfo {
        return storeAccountUseCase(
            StoreAccountUseCase.Param(
                email = email,
                response = response,
                staySignedIn = staySignedIn,
                fetchUserInfo = fetchUserInfo
            )
        ).getOrThrow()
    }

}