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
import com.nunchuk.android.model.ServerKey
import com.nunchuk.android.model.TapSigner

data class ServerKeyDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("xfp") val xfp: String? = null,
    @SerializedName("derivation_path") val derivationPath: String? = null,
    @SerializedName("xpub") val xpub: String? = null,
    @SerializedName("pubkey") val pubkey: String? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("tapsigner") val tapsigner: TapSignerDto? = null,
    @SerializedName("policies") val policies: KeyPoliciesDto? = null,
    @SerializedName("tags") val tags: List<String>? = emptyList(),
    @SerializedName("key_index") val index: Int = 0,
)

internal fun ServerKeyDto.toModel(): ServerKey {
    return ServerKey(
        name = name ?: "",
        xfp = xfp ?: "",
        derivationPath = derivationPath ?: "",
        xpub = xpub ?: "",
        pubkey = pubkey ?: "",
        id = id ?: "",
        type = type ?: "",
        tapsigner = tapsigner?.toModel() ?: TapSigner(),
        tags = tags.orEmpty(),
        index = index,
    )
}
