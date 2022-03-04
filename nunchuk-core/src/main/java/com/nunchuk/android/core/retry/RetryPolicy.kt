package com.nunchuk.android.core.retry

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException

const val DEFAULT_RETRY_POLICY = "DEFAULT_RETRY_POLICY"
const val SYNC_RETRY_POLICY = "SYNC_RETRY_POLICY"

interface RetryPolicy {
    val numRetries: Long
    val delayMillis: Long
    val delayFactor: Long
}

data class DefaultRetryPolicy(
    override val numRetries: Long = 5,
    override val delayMillis: Long = 200,
    override val delayFactor: Long = 1
) : RetryPolicy

data class SyncRetryPolicy(
    override val numRetries: Long = 10,
    override val delayMillis: Long = 200,
    override val delayFactor: Long = 2
) : RetryPolicy


fun <T> Flow<T>.retryDefault(
    retryPolicy: RetryPolicy
): Flow<T> {
    var currentDelay = retryPolicy.delayMillis
    val delayFactor = retryPolicy.delayFactor

    return retryWhen { cause, attempt ->
        if (cause is Exception && attempt < retryPolicy.numRetries) {
            delay(currentDelay)
            currentDelay *= delayFactor
            return@retryWhen true
        } else {
            return@retryWhen false
        }
    }
}

fun <T> Flow<T>.retryIO(
    retryPolicy: RetryPolicy
): Flow<T> {
    var currentDelay = retryPolicy.delayMillis
    val delayFactor = retryPolicy.delayFactor

    return retryWhen { cause, attempt ->
        if (cause is IOException && attempt < retryPolicy.numRetries) {
            delay(currentDelay)
            currentDelay *= delayFactor
            return@retryWhen true
        } else {
            return@retryWhen false
        }
    }
}
