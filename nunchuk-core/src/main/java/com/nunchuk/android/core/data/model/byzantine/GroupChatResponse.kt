package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

class GroupChatDataResponse(
    @SerializedName("chat")
    val chat: GroupChatDto
)

class GroupChatDto(
    @SerializedName("created_time_millis")
    val createdTimeMillis: Long? = null,
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("history_period")
    val historyPeriod: HistoryPeriodResponseOrRequest? = null,
    @SerializedName("room_id")
    val roomId: String? = null
)

data class HistoryPeriodResponseOrRequest(
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("enabled")
    val enabled: Boolean? = null,
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("interval")
    val interval: String? = null,
    @SerializedName("interval_count")
    val intervalCount: Int? = null
)

class HistoryPeriodResponse(
    @SerializedName("history_periods")
    val periods: List<HistoryPeriodResponseOrRequest>? = null
)

class CreateOrUpdateGroupChatRequest(
    @SerializedName("history_period_id") val historyPeriodId: String? = null,
    @SerializedName("room_id") val roomId: String? = null
)