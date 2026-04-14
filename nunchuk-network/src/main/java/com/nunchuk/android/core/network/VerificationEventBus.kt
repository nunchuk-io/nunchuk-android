package com.nunchuk.android.core.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.coroutines.cancellation.CancellationException

class VerificationEventBus {
    private val _verificationRequired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val verificationRequired: SharedFlow<Unit> = _verificationRequired.asSharedFlow()

    private var tokenDeferred: CompletableDeferred<String>? = null

    /**
     * Called from the interceptor. Signals the UI to show the verification WebView
     * and suspends until a token is provided or verification is cancelled.
     */
    suspend fun requestVerificationToken(): String {
        val deferred = CompletableDeferred<String>()
        tokenDeferred = deferred
        _verificationRequired.emit(Unit)
        return deferred.await()
    }

    fun onTokenReceived(token: String) {
        tokenDeferred?.complete(token)
        tokenDeferred = null
    }

    fun onVerificationCancelled() {
        tokenDeferred?.completeExceptionally(CancellationException("Verification cancelled"))
        tokenDeferred = null
    }

    companion object {
        private val INSTANCE = VerificationEventBus()
        fun instance() = INSTANCE
    }
}
