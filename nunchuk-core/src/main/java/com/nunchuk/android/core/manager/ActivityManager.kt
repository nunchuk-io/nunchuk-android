package com.nunchuk.android.core.manager

import android.app.Activity
import java.util.*

class ActivityManager {
    private val activityStack = Stack<Activity>()

    companion object {
        val instance = InstanceHolder.instance
    }

    private object InstanceHolder {
        var instance = ActivityManager()
    }

    fun add(activity: Activity) {
        activityStack.push(activity)
    }

    fun remove(activity: Activity) {
        activityStack.remove(activity)
    }

    fun popUntilRoot() {
        while (activityStack.size > 1) {
            activityStack.pop().let {
                activityStack.remove(it)
                it.finish()
            }
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

}

infix fun <T : Activity> Activity?.instanceOf(clazz: Class<T>) = this != null && this.javaClass == clazz
