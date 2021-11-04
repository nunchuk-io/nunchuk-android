package com.nunchuk.android.core.api

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
    val timeline: SyncStateRoomTimelineResponse? = null
)
data class SyncStateRoomTimelineResponse(
    @SerializedName("events")
    val events: List<SyncStateRoomEventResponse>? = null
)

data class SyncStateRoomEventResponse(
    @SerializedName("type")
    val type: String? = null
)