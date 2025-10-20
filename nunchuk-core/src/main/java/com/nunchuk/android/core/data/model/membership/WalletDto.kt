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
import com.nunchuk.android.model.WalletServer
import com.nunchuk.android.model.toMembershipPlan
import com.nunchuk.android.persistence.entity.AssistedWalletEntity
import com.nunchuk.android.type.WalletType

data class WalletDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("local_id") val localId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("bsms") val bsms: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("server_key") val serverKeyDto: ServerKeyDto? = null,
    @SerializedName("signers") val signerServerDtos: List<SignerServerDto> = emptyList(),
    @SerializedName("status") val status: String? = null,
    @SerializedName("created_time_millis") val createdTimeMilis: Long = 0L,
    @SerializedName("primary_membership_id") val primaryMembershipId: String? = null,
    @SerializedName("alias") val alias: String? = null,
    @SerializedName("replaced_by") val replaceBy: ReplaceByDto? = null,
    @SerializedName("remove_unused_keys") val removeUnusedKeys: Boolean = false,
    @SerializedName("hide_fiat_currency") val hideFiatCurrency: Boolean = false,
    @SerializedName("wallet_type") val walletType: String? = null,
    @SerializedName("send_bsms_email") val sendBsmsEmail: Boolean = false,
    @SerializedName("requires_registration") val requiresRegistration: Boolean = false,
)

data class ReplaceByDto(
    @SerializedName("local_id") val walletId: String? = null,
)

internal fun WalletDto.toModel(): WalletServer {
    return WalletServer(
        id = id ?: "",
        localId = localId ?: "",
        name = name ?: "",
        description = description ?: "",
        bsms = bsms ?: "",
        slug = slug ?: "",
        serverKey = serverKeyDto?.toModel(),
        signers = signerServerDtos.map { it.toModel() },
        status = status ?: "",
        createdTimeMilis = createdTimeMilis,
        primaryMembershipId = primaryMembershipId ?: "",
        alias = alias ?: "",
        walletType = walletType?.toWalletType() ?: WalletType.MULTI_SIG,
        sendBsmsEmail = sendBsmsEmail,
        requiresRegistration = requiresRegistration,
    )
}

fun String.toWalletType(): WalletType {
    return when (this) {
        "SINGLE_SIG" -> WalletType.SINGLE_SIG
        "MULTI_SIG" -> WalletType.MULTI_SIG
        "ESCROW" -> WalletType.ESCROW
        "MINISCRIPT" -> WalletType.MINISCRIPT
        else -> WalletType.MULTI_SIG
    }
}

fun WalletDto.toEntity() = AssistedWalletEntity(
    localId = localId.orEmpty(),
    plan = slug.toMembershipPlan(),
    id = id?.toLongOrNull() ?: 0L,
    status = status.orEmpty(),
    alias = alias.orEmpty(),
    hideFiatCurrency = hideFiatCurrency,
    primaryMembershipId = primaryMembershipId.orEmpty(),
    replaceByWalletId = replaceBy?.walletId.orEmpty()
)