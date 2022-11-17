/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.network.HeaderProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import javax.inject.Inject

interface MatrixInterceptor {
    fun login(username: String, password: String, encryptedDeviceId: String): Flow<Session>
}

internal class MatrixInterceptorImpl @Inject constructor(
    matrix: Matrix,
    private val matrixProvider: MatrixProvider,
    private val headerProvider: HeaderProvider,
    private val sessionHolder: SessionHolder
) : MatrixInterceptor {

    private var authenticationService = matrix.authenticationService()

    override fun login(username: String, password: String, encryptedDeviceId: String) = flow {
        emit(
            authenticationService
                .directAuthentication(
                    homeServerConnectionConfig = matrixProvider.getServerConfig(),
                    matrixId = username,
                    password = password,
                    initialDeviceName = headerProvider.getDeviceName(),
                    deviceId = encryptedDeviceId
                ).apply {
                    authenticationService.reset()
                    sessionHolder.storeActiveSession(this)
                    MatrixEvenBus.instance.publish(MatrixEvent.SignedInEvent(this))
                })
    }

}