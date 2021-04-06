package com.nunchuk.android.widget.util

import java.util.*

class NunchukCountdownTimer {

    private val timer = Timer()

    fun doAfter(job: () -> Unit, timeInMilliSecs: Long = 3000) {
        timer.schedule(NunchukTimerTask(job), timeInMilliSecs, timeInMilliSecs)
    }

    private class NunchukTimerTask(val job: () -> Unit) : TimerTask() {
        override fun run() {
            job()
        }
    }

}