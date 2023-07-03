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

import com.google.gson.annotations.SerializedName

data class KeyResponse(
    @SerializedName("key_id")
    val keyId: String,
    @SerializedName("key_checksum")
    val keyCheckSum: String,
    @SerializedName("key_backup_base64")
    val keyBackUpBase64: String,
    @SerializedName("key_checksum_algorithm")
    val keyChecksumAlgorithm: String? = null,
    @SerializedName("key_name")
    val keyName: String? = null,
    @SerializedName("key_xfp")
    val keyXfp: String? = null,
    @SerializedName("card_id")
    val cardId: String? = null,
    @SerializedName("verification_type")
    val verificationType: String? = null,
    @SerializedName("verified_time_milis")
    val verifiedTimeMilis: Long? = null,
    @SerializedName("derivation_path")
    val derivationPath: String? = null,
)