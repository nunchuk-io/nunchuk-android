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

import com.google.firebase.messaging.FirebaseMessaging
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import com.nunchuk.android.usecase.free.groupwallet.NotificationDeviceUnregisterUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ClearInfoSessionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val sessionHolder: SessionHolder,
    private val accountManager: AccountManager,
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder,
    private val ncDataStore: NcDataStore,
    private val premiumWalletRepository: PremiumWalletRepository,
    private val notificationDeviceUnregisterUseCase: NotificationDeviceUnregisterUseCase,
    private val applicationScope: CoroutineScope,
) : UseCase<Unit, Unit>(dispatcher) {

    override suspend fun execute(parameters: Unit) {
        sessionHolder.clearActiveSession()
        accountManager.signOut()
        ncDataStore.clear()
        primaryKeySignerInfoHolder.clear()
        premiumWalletRepository.clearLocalData()
        applicationScope.launch {
            runCatching { FirebaseMessaging.getInstance().token.await() }.onSuccess { token ->
                notificationDeviceUnregisterUseCase(NotificationDeviceUnregisterUseCase.Param(token))
            }
        }
    }
}