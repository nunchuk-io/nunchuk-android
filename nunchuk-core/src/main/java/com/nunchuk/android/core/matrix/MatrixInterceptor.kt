package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.network.HeaderProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

interface MatrixInterceptor {
    fun login(username: String, password: String): Flow<Session>
}

internal class MatrixInterceptorImpl @Inject constructor(
    private val matrixProvider: MatrixProvider,
    private val headerProvider: HeaderProvider
) : MatrixInterceptor {

    override fun login(username: String, password: String) = flow {
        emit(
            matrixProvider.getMatrix()
                .authenticationService()
                .directAuthentication(
                    homeServerConnectionConfig = matrixProvider.getServerConfig(),
                    matrixId = username,
                    password = password,
                    initialDeviceName = "Android ${headerProvider.getDeviceId()}"
                ).also {
                    SessionHolder.currentSession = it
                }
        )
    }

}