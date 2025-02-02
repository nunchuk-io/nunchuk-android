package com.nunchuk.android.core.data.model

import com.google.gson.annotations.SerializedName

data class DeeplinkInfo(
    @SerializedName("\$deeplink_path")
    val deeplinkPath: String,
    @SerializedName("group_id")
    val groupId: String,
    @SerializedName("~campaign")
    val campaign: String,
    @SerializedName("~channel")
    val channel: String,
    @SerializedName("~referring_link")
    val referringLink: String
)