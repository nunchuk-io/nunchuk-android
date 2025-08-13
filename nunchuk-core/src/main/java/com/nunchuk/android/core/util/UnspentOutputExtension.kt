package com.nunchuk.android.core.util

import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.type.MiniscriptTimelockBased

fun UnspentOutput.getNearestTimeLock(currentBlockHeight: Int): Long? {
    val check =
        if (lockBased == MiniscriptTimelockBased.TIME_LOCK) System.currentTimeMillis() / 1000 else currentBlockHeight.toLong()
    timelocks.find { it > check }?.let { time ->
        return time
    }
    return null
}