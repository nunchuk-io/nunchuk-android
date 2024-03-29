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

package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownperiod

import com.nunchuk.android.model.Period

sealed class LockdownPeriodEvent {
    data class Loading(val isLoading: Boolean) : LockdownPeriodEvent()
    data class ProcessFailure(val message: String) : LockdownPeriodEvent()
    data class CalculateRequiredSignaturesSuccess(
        val type: String,
        val walletId: String,
        val userData: String,
        val requiredSignatures: Int
    ) : LockdownPeriodEvent()

    data class LockdownUpdateSuccess(val period: String) : LockdownPeriodEvent()
}

data class LockdownPeriodState(
    val options: List<PeriodOption> = emptyList(),
    val userData: String? = null,
    val period: Period? = null
)

data class PeriodOption(val period: Period, val isSelected: Boolean)
