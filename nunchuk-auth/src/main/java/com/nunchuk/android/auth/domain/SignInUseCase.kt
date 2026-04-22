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

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val storeAccountUseCase: StoreAccountUseCase,
) : UseCase<SignInUseCase.Param, AccountInfo>(ioDispatcher) {

    override suspend fun execute(parameters: Param): AccountInfo {
        val response = authRepository.login(
            email = parameters.email,
            password = parameters.password
        )
        return storeAccountUseCase(
            StoreAccountUseCase.Param(
                email = parameters.email,
                response = response,
                staySignedIn = parameters.staySignedIn,
                fetchUserInfo = parameters.fetchUserInfo,
                loginType = SignInMode.EMAIL
            )
        ).getOrThrow()
    }

    data class Param(
        val email: String,
        val password: String,
        val staySignedIn: Boolean = true,
        val fetchUserInfo: Boolean = true,
    )
}
