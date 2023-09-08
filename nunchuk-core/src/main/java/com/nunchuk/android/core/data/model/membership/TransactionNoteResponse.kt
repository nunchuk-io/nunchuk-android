package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class TransactionNoteResponse(
    @SerializedName("notes") val notes: List<TransactionNoteDto> = emptyList()
)

data class TransactionNoteDto(
    @SerializedName("transaction_id")
    val transactionId: String? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("note_last_modified_time_millis")
    val modifiedTime: Long = 0L
)