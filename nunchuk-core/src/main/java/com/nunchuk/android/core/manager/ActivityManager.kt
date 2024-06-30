/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.Stack

@Deprecated("This is workaround solution, it can't restore the stack after process recreate. Use official solution instead")
object ActivityManager : Application.ActivityLifecycleCallbacks {
    private val activityStack = Stack<Activity>()

    fun peek(): Activity? = if (activityStack.isNotEmpty()) activityStack.peek() else null

    @Deprecated("Use returnToMainScreen instead")
    fun popUntilRoot() {
        while (activityStack.size > 1) {
            activityStack.pop().let {
                activityStack.remove(it)
                it.finish()
            }
        }
    }

    fun popToLevel(level: Int) {
        if (activityStack.size <= level) return
        while (activityStack.size > level) {
            activityStack.pop().finish()
        }
    }

    fun <T : Activity> popUntil(clazz: Class<T>, inclusive: Boolean = false) {
        if (activityStack.isNotEmpty()) {
            while (activityStack.isNotEmpty() && !(activityStack.lastElement() instanceOf clazz) && activityStack.size > 1) {
                activityStack.pop().finish()
            }
            if (inclusive && activityStack.isNotEmpty() && activityStack.lastElement() instanceOf clazz) {
                activityStack.pop().finish()
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityStack.push(activity)
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityStack.remove(activity)
    }
}

infix fun <T : Activity> Activity?.instanceOf(clazz: Class<T>) =
    this != null && this.javaClass == clazz
