package com.nunchuk.android.messages.util

import org.matrix.android.sdk.api.session.room.model.RoomSummary

fun List<RoomSummary>.sortByLastMessage(): List<RoomSummary> {
    return sortedByDescending { it.latestPreviewableEvent?.root?.originServerTs ?: 0 }
}

fun RoomSummary?.latestPreviewableEventTs() = this?.latestPreviewableEvent?.root?.originServerTs ?: 0
