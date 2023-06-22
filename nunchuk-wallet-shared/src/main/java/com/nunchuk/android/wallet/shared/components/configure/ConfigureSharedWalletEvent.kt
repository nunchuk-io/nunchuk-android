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

package com.nunchuk.android.wallet.shared.components.configure

sealed class ConfigureSharedWalletEvent {
    data class ConfigureCompletedEvent(
        val totalSigns: Int,
        val requireSigns: Int,
    ) : ConfigureSharedWalletEvent()
}

data class ConfigureSharedWalletState(
    val totalSigns: Int = TOTAL_SIGNS_MIN,
    val minTotalSigner : Int = TOTAL_SIGNS_MIN,
    val requireSigns: Int = 0,
    val isConfigured: Boolean = false,
    val canDecreaseTotal: Boolean = false
)

private const val TOTAL_SIGNS_MIN = 2