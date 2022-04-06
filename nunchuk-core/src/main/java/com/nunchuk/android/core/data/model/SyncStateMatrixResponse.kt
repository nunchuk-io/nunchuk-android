package com.nunchuk.android.core.data.model

import com.google.gson.annotations.SerializedName

data class SyncStateMatrixResponse(
    @SerializedName("rooms")
    val rooms: SyncStateRoomResponse? = null
)

data class SyncStateRoomResponse(
    @SerializedName("join")
    val join: Map<String, SyncStateRoomDetailResponse>? = null
)

data class SyncStateRoomDetailResponse(
    @SerializedName("timeline")
    val timeline: SyncStateRoomTimelineResponse? = null,
    @SerializedName("account_data")
    val accountData: SyncStateRoomAccountDataResponse? = null
)

data class SyncStateRoomTimelineResponse(
    @SerializedName("events")
    val events: List<SyncStateRoomEventResponse>? = null
)

data class SyncStateRoomAccountDataResponse(
    @SerializedName("events")
    val events: List<SyncStateRoomAccountDataEventResponse>? = null
)

data class SyncStateRoomAccountDataEventResponse(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("content")
    val content: SyncStateContentResponse? = null,
)
data class SyncStateContentResponse(
    @SerializedName("tags")
    val tags: Map<String, Any>? = null,
)

data class SyncStateRoomEventResponse(
    @SerializedName("type")
    val type: String? = null
)