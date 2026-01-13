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

package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

class InheritanceClaimingInitResponse(
    @SerializedName("wallet_type")
    val walletType: String? = null,
    @SerializedName("wallet_local_id")
    val walletLocalId: String? = null,
    @SerializedName("inheritance_key_count")
    val inheritanceKeyCount: Int? = null,
    @SerializedName("key_origins")
    val keyOrigins: List<KeyOriginDto>? = null
)

class KeyOriginDto(
    @SerializedName("xfp")
    val xfp: String? = null,
    @SerializedName("derivation_path")
    val derivationPath: String? = null
)

class InheritanceClaimingDownloadWalletResponse(
    @SerializedName("wallet")
    val wallet: WalletDto? = null
)

