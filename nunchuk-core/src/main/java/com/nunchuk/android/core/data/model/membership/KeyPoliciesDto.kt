package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.KeyPolicy

internal data class KeyPoliciesDto(
    @SerializedName("auto_broadcast_transaction")
    val autoBroadcastTransaction: Boolean = false,
    @SerializedName("signing_delay_seconds")
    val signingDelaySeconds: Int = 0,
    @SerializedName("spending_limit")
    val spendingLimit: SpendingPolicyDto? = null
)

internal data class SpendingPolicyDto(
    @SerializedName("interval")
    val interval: String,
    @SerializedName("limit")
    val limit: Long,
    @SerializedName("currency")
    val currency: String,
)

internal fun KeyPolicy.toDto(): KeyPoliciesDto = KeyPoliciesDto(
    autoBroadcastTransaction = autoBroadcastTransaction,
    signingDelaySeconds = signingDelayInSeconds,
    spendingLimit = spendingPolicy?.let {
        SpendingPolicyDto(
            interval = it.timeUnit.name,
            currency = it.currencyUnit.name,
            limit = it.limit,
        )
    }
)