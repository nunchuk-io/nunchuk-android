package com.nunchuk.android.core.util

import com.nunchuk.android.model.RoomTransaction
import com.nunchuk.android.model.Transaction

fun RoomTransaction.isInitialized() = initEventId.isNotEmpty()

fun RoomTransaction.isPendingSignature() = readyEventId.isEmpty()

fun RoomTransaction.isReadyBroadcast() = broadcastEventId.isNotEmpty()

fun Transaction.isPendingSignature() = signers.count { !it.value } > 0