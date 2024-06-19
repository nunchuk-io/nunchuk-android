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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface ScheduleGetPriceConvertBTCUseCase {
    fun execute(): Flow<Unit>
}

internal class ScheduleGetPriceConvertBTCUseCaseImpl @Inject constructor(
) : ScheduleGetPriceConvertBTCUseCase {

    override fun execute(
    ) = flow {
        delay(0)
        while (true) {
            delay(INTERVAL_TIME_GET_BTC_PRICE)
            emit(Unit)
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val INTERVAL_TIME_GET_BTC_PRICE = 300000L
    }
}