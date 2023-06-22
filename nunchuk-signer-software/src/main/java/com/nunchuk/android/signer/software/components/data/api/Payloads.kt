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

package com.nunchuk.android.signer.software.components.data.api

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.UserResponse
import java.io.Serializable

data class PKeySignUpPayload(
    @SerializedName("address")
    val address: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("signature")
    val signature: String
) : Serializable

data class PKeySignInPayload(
    @SerializedName("address")
    val address: String?,
    @SerializedName("username")
    val username: String,
    @SerializedName("signature")
    val signature: String
) : Serializable

data class PKeyNoncePayload(
    @SerializedName("address")
    val address: String?,
    @SerializedName("username")
    val username: String,
    @SerializedName("nonce")
    val nonce: String?
) : Serializable

data class PKeyChangeKeyPayload(
    @SerializedName("new_key")
    val newKey: String,
    @SerializedName("old_signed_message")
    val oldSignedMessage: String,
    @SerializedName("new_signed_message")
    val newSignedMessage: String
) : Serializable

data class PKeyDeleteKeyPayload(
    @SerializedName("signed_message")
    val signedMessage: String
) : Serializable

data class UserResponseWrapper(
    @SerializedName("user")
    val user: UserResponse
) : Serializable