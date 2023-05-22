package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

internal class GetGroupWalletsResponse(
    @SerializedName("groups")
    val groups: List<GroupWalletResponse>? = null
)