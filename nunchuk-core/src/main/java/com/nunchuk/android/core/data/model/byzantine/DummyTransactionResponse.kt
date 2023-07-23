package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class DummyTransactionResponse(
    @SerializedName("dummy_transaction") val dummyTransaction: DummyTransactionDto? = null,
)

data class DummyTransactionDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("wallet_id") val walletId: String? = null,
    @SerializedName("wallet_local_id") val walletLocalId: String? = null,
    @SerializedName("required_signatures") val requiredSignatures: Int = 0,
    @SerializedName("pending_signatures") val pendingSignatures: Int = 0,
    @SerializedName("request_body") val requestBody: String? = null,
    @SerializedName("requester_user_id") val requesterUserId: String? = null,
    @SerializedName("signatures") val signatures: List<SignatureDto> = emptyList(),
    @SerializedName("type") val type: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("payload") val payload: JsonObject? = null,
    @SerializedName("created_time_millis") val createdTimeMillis: Long = 0L
)

data class SignatureDto(
    @SerializedName("xfp") val xfp: String? = null,
    @SerializedName("signature") val signature: String? = null,
    @SerializedName("signed_by_user_id") val signedByUserId: String? = null,
    @SerializedName("created_time_millis") val createdTimeMillis: Long = 0L
)