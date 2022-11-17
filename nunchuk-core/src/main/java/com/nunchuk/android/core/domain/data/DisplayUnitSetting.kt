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

package com.nunchuk.android.core.domain.data


const val BTC_AND_FIXED_PRECISION = 1
const val BTC = 2
const val SAT = 3

var CURRENT_DISPLAY_UNIT_TYPE = BTC_AND_FIXED_PRECISION


data class DisplayUnitSetting(
    val useBTC: Boolean = true,
    val showBTCPrecision : Boolean = true,
    val useSAT: Boolean = false
) {
    fun getCurrentDisplayUnitType() = when {
        useBTC && showBTCPrecision -> BTC_AND_FIXED_PRECISION
        useSAT -> SAT
        else -> BTC
    }
}
