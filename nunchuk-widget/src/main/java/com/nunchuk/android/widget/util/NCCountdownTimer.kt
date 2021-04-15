package com.nunchuk.android.widget.util

import java.util.*

class NCCountdownTimer {

    private val timer = Timer()

    fun doAfter(job: () -> Unit, timeInMilliSecs: Long = 3000) {
        timer.schedule(NCTimerTask(job), timeInMilliSecs, timeInMilliSecs)
    }

    private class NCTimerTask(val job: () -> Unit) : TimerTask() {
        override fun run() {
            job()
        }
    }

}