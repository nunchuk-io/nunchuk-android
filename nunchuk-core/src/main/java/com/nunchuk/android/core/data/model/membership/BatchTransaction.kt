package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

class BatchTransactionPayload(
    @SerializedName("transactions")
    val transactions: List<TransactionPayload>
)

class TransactionPayload(
    @SerializedName("note")
    val note: String,
    @SerializedName("psbt")
    val psbt: String
)

class RandomizeBroadcastBatchTransactionsPayload(
    @SerializedName("transaction_ids")
    val transactionIds: List<String>,
    @SerializedName("days")
    val days: Int
)