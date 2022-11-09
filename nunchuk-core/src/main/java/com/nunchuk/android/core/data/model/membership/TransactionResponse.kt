package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class TransactionResponse(
    @SerializedName("transaction") val transaction: TransactionServer? = null
)

data class TransactionServer(
    @SerializedName("wallet_id") val walletId: String? = null,
    @SerializedName("wallet_local_id") val walletLocalId: String? = null,
    @SerializedName("transaction_id") val transactionId: String? = null,
    @SerializedName("psbt") val psbt: String? = null,
    @SerializedName("hex") val hex: String? = null,
    @SerializedName("reject_msg") val rejectMsg: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("created_time_milis") val createdTimeMilis: Long = 0,
    @SerializedName("signed_at_milis") val signedAtMilis: Long = 0,
    @SerializedName("last_modified_time_milis") val lastModifiedTimeMilis: Long = 0
)