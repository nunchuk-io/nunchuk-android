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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserWalletConfigsSetup(
    val walletTypes: List<String>,
    val supportedSigners: List<SupportedSignerConfig>,
    val miniscriptSupportedFirmwares: List<MiniscriptSupportedFirmware>
) : Parcelable

@Parcelize
data class SupportedSignerConfig(
    val walletType: String,
    val isInheritanceKey: Boolean,
    val signerType: String,
    val signerTag: String?
) : Parcelable

@Parcelize
data class MiniscriptSupportedFirmware(
    val signerTag: String,
    val version: String
) : Parcelable
