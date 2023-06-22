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
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.BaseUseCase
import javax.inject.Inject

interface RegisterUseCase {
    suspend fun execute(name: String, email: String): Result<UserTokenResponse>
}

internal class RegisterUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager
) : BaseUseCase(), RegisterUseCase {

    override suspend fun execute(name: String, email: String) = exe {
        authRepository.register(name = name, email = email).also {
            accountManager.storeAccount(AccountInfo(email = email, token = it.tokenId))
        }
    }
}
