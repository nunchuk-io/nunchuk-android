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

package com.nunchuk.android.core.data.model.banner

import com.google.gson.annotations.SerializedName

internal class BannerListResponse {
    @SerializedName("reminder")
    val banner: BannerDto? = null
}


internal data class BannerDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("content") val content: BannerContentDto? = null,
    @SerializedName("payload") val payload: BannerPayloadDto? = null,
)

internal data class BannerContentDto(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("action") val action: BannerActionDto? = null,
)

internal data class BannerActionDto(
    @SerializedName("label") val label: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("target") val target: String? = null,
)

internal data class BannerPayloadDto(
    @SerializedName("expiry_at_millis") val expiryAtMillis: Long? = null,
)