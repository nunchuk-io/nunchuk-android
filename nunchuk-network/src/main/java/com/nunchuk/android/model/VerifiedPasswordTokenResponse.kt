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

package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName

class VerifiedPasswordTokenResponse(
    @SerializedName("token")
    val token: Data
) {
    class Data(
        @SerializedName("token") val token: String? = null,
        @SerializedName("user_id") val userId: String? = null,
        @SerializedName("expires_at") val expiresAt: Long? = null,
        @SerializedName("target_action") val targetAction: String? = null,
    )
}