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

package com.nunchuk.android.model

/**
 * Represents the state of a wallet banner warning.
 * @param walletId The unique identifier of the wallet
 * @param state The current banner state indicating what actions are needed
 */
data class WalletBannerState(
    val walletId: String,
    val state: BannerState
)

/**
 * Enum representing the different warning states for wallet banners.
 */
enum class BannerState {
    /**
     * Please back up your wallet configuration. You might also need to register the wallet on your hardware.
     */
    BACKUP_AND_REGISTER,
    
    /**
     * Please back up your wallet configuration.
     */
    BACKUP_ONLY,
    
    /**
     * You might need to register the wallet on your hardware device.
     */
    REGISTER_ONLY
} 