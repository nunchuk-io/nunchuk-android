package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

internal data class KeyPoliciesDto(
    @SerializedName("auto_broadcast_transaction")
    val autoBroadcastTransaction: Boolean = false,
    @SerializedName("signing_delay_seconds")
    val signingDelaySeconds: Int = 0,
)