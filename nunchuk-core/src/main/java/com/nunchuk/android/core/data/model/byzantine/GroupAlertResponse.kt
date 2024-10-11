package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

data class GroupAlertResponse(
    @SerializedName("alerts")
    val alerts: List<AlertResponse>? = null
)

data class TotalAlertResponse(
    @SerializedName("total")
    val total: Int = 0
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
    val type: String? = null,
)

data class PayloadResponse(
    @SerializedName("master_name")
    val masterName: String? = null,
    @SerializedName("pending_keys_count")
    val pendingKeysCount: Int? = null,
    @SerializedName("dummy_transaction_id")
    val dummyTransactionId: String? = null,
    @SerializedName("register_key_xfps")
    val xfps: List<String> = emptyList(),
    @SerializedName("claim_key")
    val claimKey: Boolean = false,
    @SerializedName("key_xfp")
    val keyXfp: String? = null,
    @SerializedName("payment_name")
    val paymentName: String? = null,
    @SerializedName("request_id")
    val requestId: String? = null,
    @SerializedName("membership_id")
    val membershipId: String? = null,
    @SerializedName("transaction_id")
    val transactionId: String? = null,
    @SerializedName("old_email")
    val oldEmail: String? = null,
    @SerializedName("new_email")
    val newEmail: String? = null,
    @SerializedName("can_cancel")
    val canCancel: Boolean = false,
    @SerializedName("can_replace")
    val canReplace: Boolean = false,
    @SerializedName("xfp")
    val xfp: String? = null,
    @SerializedName("new_wallet_local_id")
    val newWalletId: String? = null,
)