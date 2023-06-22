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

package com.nunchuk.android.core.domain

import com.nunchuk.android.core.matrix.MatrixInterceptor
import kotlinx.coroutines.flow.Flow
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

interface LoginWithMatrixUseCase {
    fun execute(userName: String, password: String, encryptedDeviceId: String): Flow<Session>
}

internal class LoginWithMatrixUseCaseImpl @Inject constructor(
    private val interceptor: MatrixInterceptor
) : LoginWithMatrixUseCase {

    override fun execute(userName: String, password: String, encryptedDeviceId: String) = interceptor.login(
        username = userName,
        password = password,
        encryptedDeviceId = encryptedDeviceId
    )

}