/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.manager

import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nunchuk.android.widget.NCToastMessage
import java.util.*

object NcToastManager : DefaultLifecycleObserver {
    private const val DELAY = 150L
    private val queue = LinkedList<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val showMessage = Runnable {
        if (queue.isNotEmpty() && ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            val topActivity = ActivityManager.peek()
            if (topActivity is AppCompatActivity && topActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                NCToastMessage(topActivity).show(queue.remove())
            } else {
                schedule()
            }
        }
    }

    fun scheduleShowMessage(message: String) {
        queue.add(message)
        schedule()
    }

    private fun schedule() {
        handler.removeCallbacks(showMessage)
        handler.postDelayed(showMessage, DELAY)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        handler.postDelayed(showMessage, DELAY)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        handler.removeCallbacks(showMessage)
    }
}