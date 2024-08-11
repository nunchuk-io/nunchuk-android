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

package com.nunchuk.android.usecase.campaign

import com.google.gson.Gson
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.campaigns.ReferrerCode
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SaveLocalReferrerCodeUseCase @Inject constructor(
    private val repository: SettingRepository,
    private val gson: Gson,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<ReferrerCode, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: ReferrerCode) {
        repository.setReferrerCode(gson.toJson(parameters))
    }
}