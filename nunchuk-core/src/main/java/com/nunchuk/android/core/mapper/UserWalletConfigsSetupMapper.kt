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

package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.data.model.MiniscriptSupportedFirmware
import com.nunchuk.android.core.data.model.SupportedSignerConfig
import com.nunchuk.android.core.data.model.UserWalletConfigsSetupResponse
import com.nunchuk.android.model.MiniscriptSupportedFirmware as DomainMiniscriptSupportedFirmware
import com.nunchuk.android.model.SupportedSignerConfig as DomainSupportedSignerConfig
import com.nunchuk.android.model.UserWalletConfigsSetup

fun UserWalletConfigsSetupResponse.toDomain(): UserWalletConfigsSetup {
    return UserWalletConfigsSetup(
        walletTypes = walletTypes,
        supportedSigners = supportedSigners.map { it.toDomain() },
        miniscriptSupportedFirmwares = miniscriptSupportedFirmwares.map { it.toDomain() }
    )
}

fun SupportedSignerConfig.toDomain(): DomainSupportedSignerConfig {
    return DomainSupportedSignerConfig(
        walletType = walletType,
        isInheritanceKey = isInheritanceKey,
        signerType = signerType,
        signerTag = signerTag
    )
}

fun MiniscriptSupportedFirmware.toDomain(): DomainMiniscriptSupportedFirmware {
    return DomainMiniscriptSupportedFirmware(
        signerTag = signerTag,
        version = version
    )
}
