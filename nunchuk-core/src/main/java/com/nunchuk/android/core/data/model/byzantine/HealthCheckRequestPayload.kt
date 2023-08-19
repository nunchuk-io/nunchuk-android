package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

internal class HealthCheckRequestPayload(
    @SerializedName("xfp")
    val keyXfp: String? = null,
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("wallet_id")
    val walletId: String? = null,
    @SerializedName("wallet_local_id")
    val walletLocalId: String? = null,
    @SerializedName("can_health_check")
    val canHealthCheck: Boolean = false,
    @SerializedName("can_cancel")
    val canCancel: Boolean = false,
)