package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

internal class GroupDataResponse(
    @SerializedName("group")
    val data: GroupResponse? = null
)