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
import com.nunchuk.android.core.data.model.byzantine.WalletConfigDto
import com.nunchuk.android.model.byzantine.toGroupWalletType
import com.nunchuk.android.model.wallet.WalletOption

internal data class GroupAssistedWalletConfigResponse(
    @SerializedName("byzantine") val byzantine: AssistedWalletConfigResponse? = null,
    @SerializedName("byzantine_pro") val byzantinePro: AssistedWalletConfigResponse? = null,
    @SerializedName("honey_badger") val honeyBadger: AssistedWalletConfigResponse? = null,
    @SerializedName("byzantine_premier") val premier: AssistedWalletConfigResponse? = null,
    @SerializedName("finney") val finney: AssistedWalletConfigResponse? = null,
    @SerializedName("finney_pro") val finneyPro: AssistedWalletConfigResponse? = null,
    @SerializedName("iron_hand") val ironHand: AssistedWalletConfigResponse? = null,
    @SerializedName("allow_group_wallet_types") val allowGroupWalletTypes: List<String> = emptyList(),
    @SerializedName("group_wallet_types") val groupWalletTypes: List<WalletOptionDto> = emptyList(),
    @SerializedName("personal_wallet_types") val personalWalletTypes: List<WalletOptionDto> = emptyList(),
)

internal data class WalletOptionDto(
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("badge") val badge: String? = null,
    @SerializedName("recommended") val recommended: Boolean? = null,
    @SerializedName("wallet_type") val walletType: String? = null,
    @SerializedName("wallet_config") val walletConfig: WalletConfigDto? = null,
    @SerializedName("allow_software_keys") val allowSoftwareKeys: Boolean? = null,
    @SerializedName("plan_name") val planName: String? = null,
)

internal fun WalletOptionDto.toWalletOption(): WalletOption? {
    val walletType = walletType?.toGroupWalletType() ?: return null
    return WalletOption(
        slug = slug.orEmpty(),
        name = name.orEmpty(),
        description = description.orEmpty(),
        badge = badge.orEmpty(),
        recommended = recommended ?: false,
        walletType = walletType,
        allowSoftKey = allowSoftwareKeys ?: false,
        planName = planName.orEmpty(),
    )
}

