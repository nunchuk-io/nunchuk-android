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

package com.nunchuk.android.core.data.model

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.SignerServerDto

// Request Models
internal data class InheritanceClaimStatusRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    internal data class Body(
        @SerializedName("magic")
        val magic: String? = null,
        @SerializedName("bsms")
        val bsms: String? = null,
        @SerializedName("message_id")
        val messageId: String? = null
    )
}

internal data class InheritanceClaimDownloadBackupRequest(
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("hashed_bps")
    val hashedBps: List<String>? = null
)

internal data class InheritanceClaimClaimRequest(
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("psbt")
    val psbt: String? = null,
    @SerializedName("bsms")
    val bsms: String? = null
)

internal data class InheritanceClaimSigningChallengeRequest(
    @SerializedName("magic")
    val magic: String? = null
)

internal data class InheritanceClaimingInitRequest(
    @SerializedName("magic")
    val magic: String? = null
)

internal data class InheritanceClaimingDownloadWalletRequest(
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("keys")
    val keys: List<SignerServerDto> = emptyList()
)

internal data class InheritanceCancelRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    internal data class Body(
        @SerializedName("wallet")
        val walletId: String? = null,
        @SerializedName("group_id")
        val groupId: String? = null
    )
}

internal data class InheritanceByzantineRequestPlanning(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    internal data class Body(
        @SerializedName("wallet")
        val walletId: String? = null,
        @SerializedName("group_id")
        val groupId: String? = null
    )
}

internal data class InheritanceClaimCreateTransactionRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    internal data class Body(
        @SerializedName("magic")
        val magic: String? = null,
        @SerializedName("bsms")
        val bsms: String? = null,
        @SerializedName("message_id")
        val messageId: String? = null,
        @SerializedName("address")
        val address: String? = null,
        @SerializedName("fee_rate")
        val feeRate: String? = null,
        @SerializedName("amount")
        val amount: String? = null,
        @SerializedName("anti_fee_sniping")
        val antiFeeSniping: Boolean? = null,
        @SerializedName("subtract_fee_from_amount")
        val subtractFeeFromAmount: Boolean? = null,
    )
}

internal data class InheritanceCheckRequest(
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("environment")
    val environment: String? = null,
)

// Response Models
internal data class InheritanceCheckResponse(
    @SerializedName("is_valid")
    val isValid: Boolean? = null,
    @SerializedName("is_paid")
    val isPaid: Boolean? = null,
    @SerializedName("is_expired")
    val isExpired: Boolean? = null,
)

