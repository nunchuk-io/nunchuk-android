package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

internal data class WalletAliasResponse(
    @SerializedName("aliases")
    val walletAlias: List<WalletAliasDto> = emptyList()
)

internal data class WalletAliasRequest(
    @SerializedName("alias") val alias: String
)

internal data class WalletAliasDto(
    @SerializedName("membership_id") val membershipId: String? = null,
    @SerializedName("alias") val alias: String? = null,
)