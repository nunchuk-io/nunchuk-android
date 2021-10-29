package com.nunchuk.android.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch

fun <T> Flow<T>.onException(action: suspend FlowCollector<T>.(Throwable) -> Unit = {}) = catch {
    action(it)
    CrashlyticsReporter.recordException(it)
}
