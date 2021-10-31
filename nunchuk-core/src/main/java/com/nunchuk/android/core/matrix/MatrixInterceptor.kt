package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.network.HeaderProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

interface MatrixInterceptor {
    fun login(username: String, password: String, encryptedDeviceId: String): Flow<Session>
}

internal class MatrixInterceptorImpl @Inject constructor(
    private val matrixProvider: MatrixProvider,
    private val headerProvider: HeaderProvider
) : MatrixInterceptor {

    private var authenticationService = matrixProvider.getMatrix().authenticationService()

    override fun login(username: String, password: String, encryptedDeviceId: String) = flow {
        emit(
            authenticationService
                .directAuthentication(
                    homeServerConnectionConfig = matrixProvider.getServerConfig(),
                    matrixId = username,
                    password = password,
                    initialDeviceName = headerProvider.getDeviceName()
                ).apply {
                    authenticationService.reset()
                    SessionHolder.storeActiveSession(this)
                })
    }

}