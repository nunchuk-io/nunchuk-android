package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

class SimilarGroupResponse {
    @SerializedName("similar")
    val similar: List<SimilarGroupDto> = emptyList()
}

data class SimilarGroupDto(
    @SerializedName("wallet_id")
    val walletId: String? = null,
    @SerializedName("wallet_local_id")
    val walletLocalId: String? = null,
    @SerializedName("group_id")
    val groupId: String? = null,
)