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

package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.persistence.NcEncryptedPreferences
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.matrix.android.sdk.api.Matrix
import javax.inject.Inject

class MatrixInitializerUseCase @Inject constructor(
    private val instance: Matrix,
    private val accountManager: AccountManager,
    private val sessionHolder: SessionHolder,
    private val encryptedPreferences: NcEncryptedPreferences,
    private val matrixProvider: MatrixProvider,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<Unit, Unit>(dispatcher) {

    override suspend fun execute(parameters: Unit) {
        if (!accountManager.getAccount().staySignedIn) return
        val chatId = accountManager.getAccount().chatId
        if (sessionHolder.hasActiveSession()) return
        val authenticationService = instance.authenticationService()
        val session = if (authenticationService.hasAuthenticatedSessions()) {
            authenticationService
                .getLastAuthenticatedSession()
                ?.takeIf { it.myUserId == chatId }
        } else {
            encryptedPreferences.getMatrixCredential(chatId)?.let { credentials ->
                runCatching {
                    authenticationService.createSessionFromSso(
                        homeServerConnectionConfig = matrixProvider.getServerConfig(),
                        credentials = credentials,
                    )
                }.getOrNull()
            }
        }
        session?.let { sessionHolder.storeActiveSession(it) }
    }
}