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

package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.PrimaryKey
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.Chain
import com.nunchuk.android.util.FileHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class GetPrimaryKeyListUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val filerHelper: FileHelper
) : UseCase<Unit, List<PrimaryKey>>(dispatcher) {

    override suspend fun execute(parameters: Unit): List<PrimaryKey> {
        val primaryKeys = mutableListOf<PrimaryKey>()
        val storagePath = filerHelper.getOrCreateNunchukRootDir()
        val mainResult = nunchukNativeSdk.getPrimaryKeys(chain = Chain.MAIN.ordinal, storagePath = storagePath).map { it.copy(chain = Chain.MAIN) }
        val testnetResult = nunchukNativeSdk.getPrimaryKeys(chain = Chain.TESTNET.ordinal, storagePath = storagePath).map { it.copy(chain = Chain.TESTNET) }
        val signetResult = nunchukNativeSdk.getPrimaryKeys(chain = Chain.SIGNET.ordinal, storagePath = storagePath).map { it.copy(chain = Chain.SIGNET) }
        primaryKeys.addAll(mainResult)
        primaryKeys.addAll(testnetResult)
        primaryKeys.addAll(signetResult)
        return primaryKeys
    }
}