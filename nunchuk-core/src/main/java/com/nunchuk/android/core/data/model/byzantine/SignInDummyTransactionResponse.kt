package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.WalletDto

data class SignInDummyTransactionResponse(
    @SerializedName("dummy_transaction") val dummyTransaction: SignInDummyTransactionDto? = null,
    @SerializedName("wallet") val walletDto: WalletDto? = null,
    @SerializedName("token") val token: SignInTokenDto? = null,
)

data class SignInDummyTransactionDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("required_signatures") val requiredSignatures: Int = 0,
    @SerializedName("pending_signatures") val pendingSignatures: Int = 0,
    @SerializedName("signatures") val signatures: List<SignatureDto> = emptyList(),
    @SerializedName("status") val status: String? = null,
    @SerializedName("created_time_millis") val createdTimeMillis: Long = 0L,
    @SerializedName("psbt") val psbt: String? = null,
)

data class SignInTokenDto(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("tokenId") val tokenId: String? = null,
    @SerializedName("deviceId") val deviceId: String? = null,
)