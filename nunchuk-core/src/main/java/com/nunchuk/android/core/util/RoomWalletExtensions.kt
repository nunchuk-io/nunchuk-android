package com.nunchuk.android.core.util

import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.toRoomWalletData

fun RoomWallet.isInitialized() = initEventId.isNotEmpty()

fun RoomWallet.isPendingKeys() = joinEventIds.size < jsonContent.toRoomWalletData().totalSigners

fun RoomWallet.isReadyFinalize() = joinEventIds.size == jsonContent.toRoomWalletData().requireSigners

fun RoomWallet.isCreated() = finalizeEventId.isNotEmpty()

fun RoomWallet.isCanceled() = cancelEventId.isNotEmpty()