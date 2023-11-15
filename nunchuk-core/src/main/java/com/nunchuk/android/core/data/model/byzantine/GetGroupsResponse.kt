package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

internal class GetGroupsResponse(
    @SerializedName("groups")
    val groups: List<GroupResponse>? = null
)