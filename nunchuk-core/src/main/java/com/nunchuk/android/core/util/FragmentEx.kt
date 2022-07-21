package com.nunchuk.android.core.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope

fun Fragment.flowObserver(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launchWhenStarted {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            block()
        }
    }
}