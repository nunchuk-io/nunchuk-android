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

import com.google.gson.Gson
import com.nunchuk.android.core.domain.data.DeveloperSetting
import com.nunchuk.android.core.persistence.NCSharePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface GetDeveloperSettingUseCase {
    fun execute(): Flow<DeveloperSetting>
}

internal class GetDeveloperSettingUseCaseImpl @Inject constructor(
    private val ncSharedPreferences: NCSharePreferences,
    private val gson: Gson
) : GetDeveloperSettingUseCase {

    override fun execute() = gson.fromJson(
        ncSharedPreferences.developerSetting,
        DeveloperSetting::class.java
    )?.let {
        flow { emit(it) }.flowOn(Dispatchers.IO)
    } ?: flow {
        emit(DeveloperSetting(debugMode = false, matrixBasedCollaborativeWallet = false))
    }.flowOn(Dispatchers.IO)

}
