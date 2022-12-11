package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.transaction.ServerTransaction

internal data class TransactionResponse(
    @SerializedName("transaction") val transaction: TransactionServerDto? = null
)

internal data class TransactionServerDto(
    @SerializedName("wallet_id") val walletId: String? = null,
    @SerializedName("wallet_local_id") val walletLocalId: String? = null,
    @SerializedName("transaction_id") val transactionId: String? = null,
    @SerializedName("psbt") val psbt: String? = null,
    @SerializedName("hex") val hex: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("reject_msg") val rejectMsg: String? = null,
    @SerializedName("created_time_milis") val createdTimeMilis: Long = 0,
    @SerializedName("sign_time_milis") val signedAtMilis: Long = 0,
    @SerializedName("last_modified_time_milis") val lastModifiedTimeMilis: Long = 0L,
    @SerializedName("broadcast_time_milis") val broadCastTimeMillis: Long = 0L,
    @SerializedName("spending_limit_reached") val spendingLimitReach: SpendingLimitReach? = null
)

data class SpendingLimitReach(
    @SerializedName("message")
    val message: String? = null
)

internal fun TransactionResponse.toServerTransaction() = ServerTransaction(
    type = transaction?.type.orEmpty(),
    broadcastTimeInMilis = transaction?.broadCastTimeMillis ?: 0L,
    spendingLimitMessage = transaction?.spendingLimitReach?.message.orEmpty()
)