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

package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SyncCoinControlData @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val repository: PremiumWalletRepository,
) : UseCase<SyncCoinControlData.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        val serverData = repository.getCoinControlData(parameters.groupId, parameters.walletId)
        val result = nunchukNativeSdk.importCoinControlData(parameters.walletId, serverData, false)
        if (!result) {
            val deviceData = nunchukNativeSdk.exportCoinControlData(parameters.walletId)
            repository.uploadCoinControlData(parameters.groupId, parameters.walletId, deviceData)
        }
    }

    data class Param(
        val groupId: String?,
        val walletId: String
    )
}