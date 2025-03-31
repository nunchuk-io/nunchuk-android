package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

internal data class DeletedGroupWalletsResponse(
    @SerializedName("wallet_local_ids")
    val walletLocalIds: List<String>
)