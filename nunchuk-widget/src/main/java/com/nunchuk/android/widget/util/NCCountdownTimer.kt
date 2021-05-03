package com.nunchuk.android.widget.util

import com.nunchuk.android.utils.Disposable
import java.util.*

class NCCountdownTimer : Timer(), Disposable {

    private var task: NCTimerTask? = null

    fun doAfter(job: () -> Unit, timeInMilliSecs: Long = 3000) {
        val task = NCTimerTask(job)
        schedule(task, timeInMilliSecs, timeInMilliSecs)
    }

    override fun dispose() {
        task?.cancel()
        cancel()
    }

}

class NCTimerTask(val job: () -> Unit = {}) : TimerTask() {
    override fun run() {
        job()
    }

}