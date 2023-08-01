package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

data class GroupAlertResponse(
    @SerializedName("alerts")
    val alerts: List<AlertResponse>? = null
)

data class TotalAlertResponse(
    @SerializedName("total")
    val total: Int? = null
)

data class AlertResponse(
    @SerializedName("viewable")
    val viewable: Boolean? = null,
    @SerializedName("body")
    val body: String? = null,
    @SerializedName("created_time_millis")
    val createdTimeMillis: Long? = null,
    @SerializedName("payload")
    val payload: PayloadResponse? = null,
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("type")
    val type: String? = null
)

data class PayloadResponse(
    @SerializedName("master_name")
    val masterName: String? = null,
    @SerializedName("pending_keys_count")
    val pendingKeysCount: Int? = null,
    @SerializedName("dummy_transaction_id")
    val dummyTransactionId: String? = null,
)