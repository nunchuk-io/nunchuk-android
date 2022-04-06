package com.nunchuk.android.core.util

import java.util.concurrent.atomic.AtomicInteger

object AppUpdateStateHolder {
    val countShowingRecommend = AtomicInteger(0)

    fun reset() {
        countShowingRecommend.set(0)
    }
}