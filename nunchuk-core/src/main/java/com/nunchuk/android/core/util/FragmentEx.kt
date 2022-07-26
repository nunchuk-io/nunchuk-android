package com.nunchuk.android.core.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

fun <T> Fragment.stateFlowObserver(flow: Flow<T>, collector: FlowCollector<T>) {
    viewLifecycleOwner.lifecycleScope.launchWhenStarted {
        flow.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect(collector)
    }
}

fun <T> Fragment.sharedFlowObserver(flow: Flow<T>, collector: FlowCollector<T>) {
    viewLifecycleOwner.lifecycleScope.launch {
        flow.collect(collector)
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
