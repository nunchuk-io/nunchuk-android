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
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.KeyPolicy

internal data class KeyPoliciesDto(
    @SerializedName("auto_broadcast_transaction")
    val autoBroadcastTransaction: Boolean = false,
    @SerializedName("signing_delay_seconds")
    val signingDelaySeconds: Int = 0,
    @SerializedName("spending_limit")
    val spendingLimit: SpendingPolicyDto? = null,
    @SerializedName("apply_same_spending_limit")
    val applySamePendingLimit: Boolean? = null,
    @SerializedName("members_spending_limit")
    val membersSpendingLimit: List<MemberSpendingLimitDto>? = null,
)

internal data class MemberSpendingLimitDto(
    @SerializedName("membership_id")
    val membershipId: String? = null,
    @SerializedName("spending_limit")
    val spendingLimit: SpendingPolicyDto? = null,
)

internal data class SpendingPolicyDto(
    @SerializedName("interval")
    val interval: String,
    @SerializedName("limit")
    val limit: Double,
    @SerializedName("currency")
    val currency: String,
)

internal fun KeyPolicy.toDto(): KeyPoliciesDto = KeyPoliciesDto(
    autoBroadcastTransaction = autoBroadcastTransaction,
    signingDelaySeconds = signingDelayInSeconds,
    spendingLimit = spendingPolicy?.let {
        SpendingPolicyDto(
            interval = it.timeUnit.name,
            currency = it.currencyUnit,
            limit = it.limit,
        )
    }
)

internal fun GroupKeyPolicy.toDto(): KeyPoliciesDto = if (isApplyAll) {
    val spendingPolicy = spendingPolicies.values.first()
    KeyPoliciesDto(
        autoBroadcastTransaction = autoBroadcastTransaction,
        signingDelaySeconds =signingDelayInSeconds,
        spendingLimit = spendingPolicy.let {
            SpendingPolicyDto(
                interval = it.timeUnit.name,
                currency = it.currencyUnit,
                limit = it.limit,
            )
        },
        applySamePendingLimit = true,
    )
} else {
    KeyPoliciesDto(autoBroadcastTransaction = autoBroadcastTransaction,
        signingDelaySeconds = signingDelayInSeconds,
        applySamePendingLimit = false,
        membersSpendingLimit = spendingPolicies.map { (memberId, spendingPolicy) ->
            MemberSpendingLimitDto(
                spendingLimit = SpendingPolicyDto(
                    interval = spendingPolicy.timeUnit.name,
                    currency = spendingPolicy.currencyUnit,
                    limit = spendingPolicy.limit,
                ), membershipId = memberId
            )
        })
}