package com.nunchuk.android.messages.util

import com.nunchuk.android.model.RoomWallet
import org.matrix.android.sdk.api.session.room.model.RoomSummary

fun List<RoomSummary>.sortByLastMessage(roomWallets: List<RoomWallet>): List<RoomSummary> {
    val roomWalletIds = roomWallets.map { it.roomId }.toSet()
    return sortedWith(compareByDescending<RoomSummary> { (it.roomId in roomWalletIds) }
        .thenBy { -(it.latestPreviewableEvent?.root?.originServerTs ?: 0L) }
    )
}

fun RoomSummary?.latestPreviewableEventTs() = this?.latestPreviewableEvent?.root?.originServerTs ?: 0L
