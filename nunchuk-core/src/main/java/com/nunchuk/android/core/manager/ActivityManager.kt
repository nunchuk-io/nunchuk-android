package com.nunchuk.android.core.manager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.Stack

object ActivityManager : Application.ActivityLifecycleCallbacks {
    private val activityStack = Stack<Activity>()

    fun peek(): Activity = activityStack.peek()

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
            while (activityStack.isNotEmpty() && !(activityStack.lastElement() instanceOf clazz)) {
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
