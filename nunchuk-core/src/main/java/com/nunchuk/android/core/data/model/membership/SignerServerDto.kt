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
import com.nunchuk.android.core.util.toSignerType
import com.nunchuk.android.model.KeyResponse
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.model.toVerifyType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

data class SignerServerDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("xfp") val xfp: String? = null,
    @SerializedName("derivation_path") val derivationPath: String? = null,
    @SerializedName("xpub") val xpub: String? = null,
    @SerializedName("pubkey") val pubkey: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("tapsigner") val tapsigner: TapSignerDto? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("user_key") val userKey: KeyResponse? = null,
    @SerializedName("key_index") val index: Int = 0,
    @SerializedName("is_visible") val isVisible: Boolean = true,
    @SerializedName("verification_type") val verificationType: String? = null,
)

internal fun SignerServerDto.toModel(): SignerServer {
    val signerType = type.toSignerType()
    return SignerServer(
        name = name,
        xfp = xfp,
        derivationPath = derivationPath,
        type = signerType,
        index = index,
        userKeyId = userKey?.keyId,
        verifyType = if (signerType == SignerType.NFC || tags.orEmpty().contains(SignerTag.INHERITANCE.name)) userKey?.verificationType.toVerifyType(verificationType) else VerifyType.APP_VERIFIED,
        isVisible = isVisible,
        tapsigner = tapsigner?.toModel(),
        xpub = xpub,
        pubkey = pubkey,
        tags = tags ?: emptyList(),
        userBackUpFileName = userKey?.fileName
    )
}

