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

package com.nunchuk.android.usecase.miniscript

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.TimeLock
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetTimelockTemplateUseCase @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<TimeLock, String>(ioDispatcher) {

    override suspend fun execute(parameters: TimeLock): String {
        val timeUnit = when (parameters.based) {
            MiniscriptTimelockBased.TIME_LOCK -> 0
            MiniscriptTimelockBased.HEIGHT_LOCK -> 1
            else -> 0
        }
        return nunchukNativeSdk.getTimelockTemplate(timelockType = parameters.type.ordinal, timeUnit = timeUnit, time = parameters.value)
    }
}
