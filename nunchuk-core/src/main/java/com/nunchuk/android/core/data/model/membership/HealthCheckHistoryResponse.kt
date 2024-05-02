package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

class HealthCheckHistoryResponseData(
    @SerializedName("history")
    val history: List<HealthCheckHistoryResponse>? = null
)

class HealthCheckHistoryResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("created_time_millis")
    val createdTimeMillis: Long? = null,
    @SerializedName("payload")
    val payload: HealthCheckHistoryPayload? = null,
)

class HealthCheckHistoryPayload(
    @SerializedName("dummy_transaction_id")
    val dummyTransactionId: String? = null,
    @SerializedName("wallet_local_id")
    val walletLocalId: String? = null,
    @SerializedName("wallet_id")
    val walletId: String? = null,
)