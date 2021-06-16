package com.nunchuk.android.core.matrix

import com.nunchuk.android.network.HeaderProvider
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

interface MatrixInterceptor {
    suspend fun login(username: String, password: String): Session
}

internal class MatrixInterceptorImpl @Inject constructor(
    private val matrixProvider: MatrixProvider,
    private val headerProvider: HeaderProvider
) : MatrixInterceptor {

    override suspend fun login(username: String, password: String) = matrixProvider.getMatrix()
        .authenticationService()
        .directAuthentication(
            homeServerConnectionConfig = matrixProvider.getServerConfig(),
            matrixId = username,
            password = password,
            initialDeviceName = "Android ${headerProvider.getDeviceId()}"
        ).also {
            SessionHolder.currentSession = it
        }

}