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

package com.nunchuk.android.model.banner

data class Banner(
    val id: String,
    val type: Type,
    val content: Content,
    val payload: Payload,
) {
    data class Content(
        val title: String,
        val description: String,
        val imageUrl: String,
        val action: Action,
    )

    data class Action(
        val label: String,
        val type: String,
        val target: String,
    )

    data class Payload(
        val expiryAtMillis: Long,
    )

    enum class Type {
        TYPE_01,
        TYPE_REFERRAL_01,
    }
}

fun String.toBannerType(): Banner.Type {
    return when (this) {
        "TYPE_01" -> Banner.Type.TYPE_01
        "TYPE_REFERRAL_01" -> Banner.Type.TYPE_REFERRAL_01
        else -> Banner.Type.TYPE_01
    }
}