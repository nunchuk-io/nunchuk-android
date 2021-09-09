package com.nunchuk.android.core.matrix

import android.content.Context
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration
import javax.inject.Inject

class MatrixInitializer @Inject constructor(private val context: Context) {

    fun initialize() {
        Matrix.initialize(
            context = context,
            matrixConfiguration = MatrixConfiguration(
                roomDisplayNameFallbackProvider = RoomDisplayNameFallbackProviderImpl()
            )
        )
        val matrix = Matrix.getInstance(context)
        val lastSession = matrix.authenticationService().getLastAuthenticatedSession()
        if (lastSession != null) {
            SessionHolder.storeActiveSession(lastSession)
        }
    }

}