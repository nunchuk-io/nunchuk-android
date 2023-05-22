package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

internal class GroupWalletDataResponse(
    @SerializedName("group")
    val data: GroupWalletResponse? = null
)