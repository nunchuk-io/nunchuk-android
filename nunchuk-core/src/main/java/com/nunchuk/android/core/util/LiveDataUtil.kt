package com.nunchuk.android.core.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.observe(owner: LifecycleOwner, observer: T.() -> Unit) {
    observe(owner, Observer { it?.let(observer::invoke) })
}