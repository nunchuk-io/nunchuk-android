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

package com.nunchuk.android.core.profile

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserResponseWrapper(
    @SerializedName("user")
    val user: UserProfileResponse
) : Serializable

data class UserProfileResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("gender")
    val gender: String? = null,
    @SerializedName("avatar")
    val avatar: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("chat_id")
    val chatId: String? = null
) : Serializable

data class UpdateUserProfilePayload(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null
) : Serializable

data class DeleteConfirmationPayload(
    @SerializedName("confirmation_code")
    val confirmationCode: String
) : Serializable