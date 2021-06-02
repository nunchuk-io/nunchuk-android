package com.nunchuk.android.extensions

import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.type.TransactionStatus.*

fun TransactionStatus.canBroadCast() = this == READY_TO_BROADCAST

fun TransactionStatus.isCompleted() = this == CONFIRMED || this == NETWORK_REJECTED || this == REPLACED
