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

package com.nunchuk.android.core.account

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.guestmode.SignInMode

data class AccountInfo(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("token")
    val token: String = "",
    @SerializedName("chatId")
    val chatId: String = "",
    @SerializedName("activated")
    val activated: Boolean = false,
    @SerializedName("staySignedIn")
    val staySignedIn: Boolean = false,
    @SerializedName("avatar_url")
    val avatarUrl: String? = "",
    @SerializedName("device_id")
    val deviceId: String? = "",
    @SerializedName("login_type")
    val loginType: Int = SignInMode.UNKNOWN.value,
    @SerializedName("username")
    val username: String = "",
    @SerializedName("primary_key_info")
    val primaryKeyInfo: PrimaryKeyInfo? = null,
    @SerializedName("decoy_pin")
    val decoyPin: String = "" // it mean user is using decoy pin space, not the real pin
)

data class PrimaryKeyInfo(
    @SerializedName("xfp")
    val xfp: String = ""
)
