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
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

const val CONF_TARGET_PRIORITY = 2
const val CONF_TARGET_STANDARD = 6
const val CONF_TARGET_ECONOMICAL = 144

class EstimateFeeUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val repository: TransactionRepository,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<Unit, EstimateFeeRates>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): EstimateFeeRates {
        return try {
            repository.getFees()
        } catch (e: Exception) {
            Timber.e(e)
            EstimateFeeRates(
                priorityRate = nativeSdk.estimateFee(CONF_TARGET_PRIORITY).value.toInt(),
                standardRate = nativeSdk.estimateFee(CONF_TARGET_STANDARD).value.toInt(),
                economicRate = nativeSdk.estimateFee(CONF_TARGET_ECONOMICAL).value.toInt()
            )
        }
    }
}