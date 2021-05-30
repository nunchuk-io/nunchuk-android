package com.nunchuk.android.extensions

import com.nunchuk.android.type.TransactionStatus

fun TransactionStatus.canBroadCast() = this == TransactionStatus.READY_TO_BROADCAST