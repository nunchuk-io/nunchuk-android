/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.auth.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserTokenResponse(
    @SerializedName("tokenId")
    val tokenId: String,
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("expireInSeconds")
    val expireInSeconds: Long = 0
) : Serializable

data class TryLoginResponse(
    @SerializedName("user_agent")
    val userAgent: String? = null,
    @SerializedName("ip")
    val ip: String? = null,
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("uuid")
    val uuid: String? = null,
    @SerializedName("expires_in_secs")
    val expiresInSecs: Int = 0,
)

data class UserResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("gender")
    val gender: String,
    @SerializedName("avatar")
    val avatar: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("chat_id")
    val chatId: String,
) : Serializable