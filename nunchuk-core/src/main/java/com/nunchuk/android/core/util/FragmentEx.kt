package com.nunchuk.android.core.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.coroutines.CoroutineScope

fun Fragment.flowObserver(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launchWhenStarted {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            block()
        }
    }
}

fun Fragment.showError(message: String?) {
    if (message.isNullOrEmpty().not()) {
        NCToastMessage(requireActivity()).showError(message.orEmpty())
    }
}

fun Fragment.showWarning(message: String?) {
    if (message.isNullOrEmpty().not()) {
        NCToastMessage(requireActivity()).showWarning(message.orEmpty())
    }
}
