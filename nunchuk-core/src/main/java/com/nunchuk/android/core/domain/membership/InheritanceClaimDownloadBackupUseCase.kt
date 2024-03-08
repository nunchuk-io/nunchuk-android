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

package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.SecurityQuestion
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class InheritanceClaimDownloadBackupUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<InheritanceClaimDownloadBackupUseCase.Param, List<BackupKey>>(dispatcher) {
    override suspend fun execute(parameters: Param): List<BackupKey> {
        val hashedBps = arrayListOf<String>()
        parameters.backupPasswords.forEach {
            hashedBps.add(nunchukNativeSdk.hashSHA256(nunchukNativeSdk.hashSHA256(it)))
        }
        return userWalletsRepository.inheritanceClaimDownloadBackup(magic = parameters.magic, hashedBps = hashedBps)
    }

    class Param(val magic: String, val backupPasswords: List<String>)
}