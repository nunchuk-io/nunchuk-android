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

data class GroupAssistedWalletConfigResponse(
    @SerializedName("byzantine") val byzantine: AssistedWalletConfigResponse? = null,
    @SerializedName("byzantine_pro") val byzantinePro: AssistedWalletConfigResponse? = null,
    @SerializedName("honey_badger") val honeyBadger: AssistedWalletConfigResponse? = null,
    @SerializedName("byzantine_premier") val premier: AssistedWalletConfigResponse? = null,
    @SerializedName("finney") val finney: AssistedWalletConfigResponse? = null,
    @SerializedName("finney_pro") val finneyPro: AssistedWalletConfigResponse? = null,
    @SerializedName("allow_group_wallet_types") val allowGroupWalletTypes: List<String> = emptyList(),
)