package com.nunchuk.android.core.matrix

import android.content.Context
import com.nunchuk.android.utils.CrashlyticsReporter
import org.matrix.android.sdk.api.Matrix
import javax.inject.Inject

class MatrixInitializer @Inject constructor(private val context: Context) {

    fun initialize() {
        val instance = Matrix.getInstance(context)
        val authenticationService = instance.authenticationService()
        try {
            if (authenticationService.hasAuthenticatedSessions()) {
                authenticationService
                    .getLastAuthenticatedSession()
                    ?.let(SessionHolder::storeActiveSession)
            }
        } catch (t: Throwable) {
            CrashlyticsReporter.recordException(t)
        }
    }

}